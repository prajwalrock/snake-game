package com.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sample.ui.theme.SnakeGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = Game(lifecycleScope)

        setContent {
            SnakeGameTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Snake(game)
                }
            }
        }
    }
}


data class State(val food: Pair<Int, Int>, val snake: List<Pair<Int, Int>>)

class Game(private val scope: CoroutineScope) {

   private val mutex = Mutex()


    private val mutableState: MutableStateFlow<State> =
        MutableStateFlow(State(food = Pair(5,5), snake = listOf(Pair(7,7))))

    val state: Flow<State> = mutableState

    var move: Pair<Int,Int> = Pair(1,0)

    set(value){
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    init {
        scope.launch {
            var snakeLength = 4

            while (true){
                delay(150)
                mutableState.update {
                    val newPosition: Pair<Int,Int> = it.snake.first().let { poz ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first + BOARD_SIZE) % BOARD_SIZE,
                                (poz.second + move.second + BOARD_SIZE) % BOARD_SIZE,
                            )
                        }
                    }

                    if(newPosition == it.food){
                        snakeLength++
                    }
                    if(it.snake.contains(newPosition)){
                        snakeLength = 4
                    }

                    it.copy(
                        food = if(newPosition == it.food) Pair(
                            Random().nextInt(BOARD_SIZE),
                            Random().nextInt(BOARD_SIZE)
                        ) else it.food,
                        snake = listOf(newPosition) + it.snake.take(snakeLength - 1)
                    )
                }
            }

        }
    }

    companion object{
        const val BOARD_SIZE = 16
    }

}

@Composable
fun Board(state: State) {

     BoxWithConstraints(Modifier.padding(16.dp)) {
         val tileSize = maxWidth / Game.BOARD_SIZE
         
         Box(modifier = Modifier
             .size(maxWidth)
             .border(2.dp, Color.Green)
         )
         
         Box(
             Modifier
                 .offset(x = tileSize * state.food.first, y = tileSize * state.food.second)
                 .size(tileSize)
                 .background(
                     Color.DarkGray, CircleShape
                 ) )

         state.snake.forEach{
             Box(modifier = Modifier
                 .offset(
                     x = tileSize * it.first,
                     y = tileSize * it.second
                 )
                 .size(tileSize)
                 .background(
                     Color.Green,
                     CircleShape
                 )
             )
         }
     }

}

@Composable
fun Snake(game: Game)  {

    val state = game.state.collectAsState(initial = null)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.value?.let {
            Board(it)
        }

        Buttons{
            game.move = it
        }
    }
    
}

@Composable
fun Buttons(onDirectionChange: (Pair<Int,Int>) -> Unit) {
    val buttonSize = Modifier.size(50.dp).background(Color.LightGray)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Button(onClick = { onDirectionChange(Pair(0,-1)) }, modifier = buttonSize ) {
            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
        }

        Row {
            Button(onClick = { onDirectionChange(Pair(-1,0)) }, modifier = buttonSize ) {
                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = null)
            }
            Spacer(modifier = buttonSize)

            Button(onClick = { onDirectionChange(Pair(1,0)) }, modifier = buttonSize ) {
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }

        Button(onClick = { onDirectionChange(Pair(0,1)) }, modifier = buttonSize ) {
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
        }

    }
}

