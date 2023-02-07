package com.udacity.project4.locationreminders.savereminder

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand

@Composable
fun ReminderTitle() {

    var text by remember { mutableStateOf(TextFieldValue("")) }
    TextField(
        value = text,
        onValueChange = {
            text = it
        },
        label = { Text(text = "Your Label") },
        placeholder = { Text(text = "Your Placeholder/Hint") },
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
        ),
        textStyle = TextStyle(color = MaterialTheme.colors.primary,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif),
        maxLines = 1,
        singleLine = true,
    )
}
//    TextField(
//
//        // inactive color is use to change
//        // color when text field is not focused.
//        inactiveColor = MaterialTheme.colors.onBackground,
//
//        // below line is use to specify background
//        // color for our text field.
//        backgroundColor = MaterialTheme.colors.background,

//    )

//}

@Composable
fun ReminderDescription() {

    var text by remember { mutableStateOf(TextFieldValue("")) }
    TextField(
        value = text,
        onValueChange = {
            text = it
        },
        label = { Text(text = "Your Label") },
        placeholder = { Text(text = "Your Placeholder/Hint") },
        modifier = Modifier
            .padding(all = 16.dp)
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
        ),
        textStyle = TextStyle(color = MaterialTheme.colors.primary,
            fontSize = 15.sp,
            fontFamily = FontFamily.SansSerif),
        maxLines = 4,
        singleLine = false,
    )
}


//@Preview
@Composable
private fun ReminderTitlePreview() {
    MaterialTheme {
        ReminderTitle()
    }
}

@Composable
private fun SelectLocation() {
    Text(
        text = "Reminder Location",
        style = MaterialTheme.typography.h5,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .wrapContentWidth(Alignment.Start)
            .clickable { gotoSelectedLocation() }

    )
}

fun gotoSelectedLocation(){
    NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
}

@Composable
private fun SelectedLocation() {
    Text(
        text = "Location",
        style = MaterialTheme.typography.h5,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .wrapContentWidth(Alignment.End)

    )
}

@Composable
private fun SaveLocation() {
    FloatingActionButton(
        onClick = {
            //OnClick Method
        },
        backgroundColor = MaterialTheme.colors.secondary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .padding(vertical = 160.dp, )
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add FAB",
        )
    }
}

@Composable
fun ReminderContent() {
    Surface {
        Column(Modifier.padding(dimensionResource(R.dimen.margin_normal)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,) {
            ReminderTitle()
            ReminderDescription()
            Row(){
                SelectLocation()
                SelectedLocation()
            }
            SaveLocation()
        }
    }
}


@Preview
@Composable
private fun ReminderContentPreview() {

    MaterialTheme {
        ReminderContent()
    }
}