package br.edu.puc.pi3_time1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.puc.pi3_time1.ui.theme.Pi3_time1Theme
import androidx.compose.ui.text.TextStyle

class CategoriesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Pi3_time1Theme {
                Categories(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                        .width(412.dp)
                        .height(917.dp)
                        .background(color = Color(0xFFFFFFFF))
                ,
                    onNavigateToMainActivity = {
                        startActivity(Intent(this@CategoriesActivity, MainActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun Categories(modifier: Modifier = Modifier, onNavigateToMainActivity: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),// Reduz o espaço superior para simular a posição da imagem 2
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título "Categorias" com posição ajustada
        Text(
            text = "Categorias",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 30.dp) // Espaçamento inferior para separar do próximo elemento
                .width(412.dp)
                .height(36.dp)// Largura fixa para centralização visual
        )

        // Campo Sites Web
        TextField(
            value = "Sites Web",
            onValueChange = { /* Não editável */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .border(2.dp, Color(0xFF000000))
                .background(Color((0xFFFFFFFF)))
                .width(362.dp)
                .height(59.dp),
            enabled = false,
            textStyle = TextStyle(
                textAlign = TextAlign.Left,
                fontSize = 20.sp,
                color = Color.Black
            )
        )
        Spacer(modifier = Modifier.height(51.dp))
        // Campo Aplicativos
        TextField(
            value = "Aplicativos",
            onValueChange = { /* Não editável */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .border(2.dp,Color(0xFF000000))
                .background(Color((0xFFFFFFFF)))
                .width(362.dp)
                .height(59.dp),
            enabled = false,
            textStyle = TextStyle(
                textAlign = TextAlign.Left,
                fontSize = 20.sp,
                color = Color.Black
            )

        )
        Spacer(modifier = Modifier.height(44.dp))
        // Campo Teclados de Acesso Físico
        TextField(
            value = "Teclados de Acesso Físico",
            onValueChange = { /* Não editável */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .border(2.dp,Color(0xFF000000))
                .background(Color((0xFFFFFFFF)))
                .width(362.dp)
                .height(59.dp),
            enabled = false,
            textStyle = TextStyle(
                textAlign = TextAlign.Left,
                fontSize = 20.sp,
                color = Color.Black
            )
        )

        // Botão Retornar
        Button(
            onClick = { onNavigateToMainActivity() },
            Modifier
                .border(width = 2.dp,Color(0xFF000000), shape = RoundedCornerShape(size = 100.dp))
                .padding(1.dp)
                .width(103.dp)
                .height(40.dp)
                .background(color = Color(0xFFFFFFFF), shape = RoundedCornerShape(size = 100.dp))
        ) {
            Text(
                text = "Retornar",
                Modifier
                    .width(55.dp)
                    .height(20.dp)

                )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoriesPreview() {
    Pi3_time1Theme {
        Categories(
            onNavigateToMainActivity = {}
        )
    }
}