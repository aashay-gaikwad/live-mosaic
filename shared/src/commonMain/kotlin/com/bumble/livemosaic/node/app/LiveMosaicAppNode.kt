package com.bumble.livemosaic.node.app

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.bumble.appyx.components.backstack.BackStack
import com.bumble.appyx.components.backstack.BackStackModel
import com.bumble.appyx.components.backstack.operation.replace
import com.bumble.appyx.navigation.composable.AppyxComponent
import com.bumble.appyx.navigation.integration.LocalScreenSize
import com.bumble.appyx.navigation.modality.BuildContext
import com.bumble.appyx.navigation.node.Node
import com.bumble.appyx.navigation.node.ParentNode
import com.bumble.appyx.navigation.node.node
import com.bumble.appyx.utils.multiplatform.Parcelable
import com.bumble.appyx.utils.multiplatform.Parcelize
import com.bumble.livemosaic.appyx.component.backstackclipper.BackStackClipper
import com.bumble.livemosaic.composable.AutoPlayScript
import com.bumble.livemosaic.composable.CallToActionScreen
import com.bumble.livemosaic.model.Puzzle.PUZZLE1
import com.bumble.livemosaic.model.Puzzle.PUZZLE2
import com.bumble.livemosaic.model.Puzzle.PUZZLE3
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget.CallToAction
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget.Puzzle1
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget.Puzzle2
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget.Puzzle3
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget.StackedMessages
import com.bumble.livemosaic.node.app.LiveMosaicAppNode.NavTarget.StarField
import com.bumble.livemosaic.node.messages.StackedMessagesNode
import com.bumble.livemosaic.node.puzzle1.Puzzle1Node
import com.bumble.livemosaic.node.starfield.StarFieldNode
import com.bumble.livemosaic.ui.DottedMeshShape
import com.bumble.livemosaic.ui.LocalAutoPlayFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private val screens = listOf(
    Puzzle1,
    Puzzle2,
    Puzzle3,
    CallToAction,
    StarField,
    StackedMessages,
)

class LiveMosaicAppNode(
    buildContext: BuildContext,
    private val backStack: BackStack<NavTarget> = BackStack(
        model = BackStackModel(
            initialTargets = listOf(screens.first()),
            savedStateMap = buildContext.savedStateMap,
        ),
        visualisation = { BackStackClipper(it, shape = { progress -> clipShape(progress) }) }
    )
) : ParentNode<NavTarget>(
    buildContext = buildContext,
    appyxComponent = backStack
) {
    private var screenIdx = 0

    sealed class NavTarget : Parcelable {
        @Parcelize
        object Puzzle1 : NavTarget()

        @Parcelize
        object Puzzle2 : NavTarget()

        @Parcelize
        object Puzzle3 : NavTarget()

        @Parcelize
        object StackedMessages : NavTarget()

        @Parcelize
        object CallToAction : NavTarget()

        @Parcelize
        object StarField : NavTarget()
    }


    override fun resolve(navTarget: NavTarget, buildContext: BuildContext): Node =
        when (navTarget) {
            is Puzzle1 -> Puzzle1Node(
                puzzle = PUZZLE1,
                buildContext = buildContext
            )

            is Puzzle2 -> Puzzle1Node(
                puzzle = PUZZLE2,
                buildContext = buildContext
            )

            is Puzzle3 -> Puzzle1Node(
                puzzle = PUZZLE3,
                buildContext = buildContext
            )

            is CallToAction -> node(buildContext) { modifier ->
                AutoPlayScript(initialDelayMs = 5000) { nextScreen() }
                CallToActionScreen(modifier)
            }

            is StarField -> StarFieldNode(buildContext)

            is StackedMessages -> StackedMessagesNode(buildContext)
        }

    override fun onChildFinished(child: Node) {
        nextScreen()
    }

    @Composable
    override fun View(modifier: Modifier) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            var autoPlayFlow = remember { MutableStateFlow(true) }

            CompositionLocalProvider(
                LocalAutoPlayFlow provides autoPlayFlow
            ) {
                CurrentScreen()
                Row {
                    AutoPlayToggle(autoPlayFlow)
                    NextButton()
                }
            }
        }
    }

    @Composable
    private fun CurrentScreen() {
        AppyxComponent(
            appyxComponent = backStack,
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun AutoPlayToggle(autoPlayFlow: MutableStateFlow<Boolean>) {
        val isAutoPlayOn = autoPlayFlow.collectAsState().value

        Button(
            onClick = { autoPlayFlow.update { !it } },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Icon(
                imageVector = if (isAutoPlayOn) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Toggle auto-play",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.alpha(if (isAutoPlayOn) 0.035f else 1f),
            )
        }
    }

    @Composable
    private fun NextButton() {
        if (!LocalAutoPlayFlow.current.collectAsState().value) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                onClick = { nextScreen() }
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next screen",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    private fun nextScreen() {
        backStack.replace(
            target = screens[++screenIdx % screens.size],
            animationSpec = tween(
                durationMillis = 3000,
                easing = FastOutLinearInEasing
            )
        )
    }
}

@Composable
private fun clipShape(progress: Float): Shape {
    val screenSize = LocalScreenSize.current
    val (meshMin, meshMax) = 14 to 25
    val meshSizeX = if (screenSize.widthDp > screenSize.heightDp) meshMax else meshMin
    val meshSizeY = if (screenSize.widthDp > screenSize.heightDp) meshMin else meshMax

    val shape by remember(progress) {
        mutableStateOf(
            DottedMeshShape(
                meshSizeX,
                meshSizeY,
                progress
            )
        )
    }

    return shape
}
