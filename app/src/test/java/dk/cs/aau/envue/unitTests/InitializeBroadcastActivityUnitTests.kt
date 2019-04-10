package dk.cs.aau.envue.unitTests

import dk.cs.aau.envue.InitializeBroadcastActivity
import org.junit.Assert
import org.junit.Test

class InitializeBroadcastActivityUnitTests {

    @Test
    fun addition_isCorrect() {
        Assert.assertEquals(4, 2 + 2)
    }

//    @Test
//    fun emojiStringToArray_emptyString_emptyArray() {
//        val str = ""
//        val expected = Array(0) {""}
//        val actual = InitializeBroadcastActivity().emojiStringToArray(str).toArray()
//        Assert.assertArrayEquals(expected, actual)
//    }
//
//    @Test
//    fun emojiStringToArray_simpleEmoji_singletonArray() {
//        val emojiStr = "\uD83D\uDE00"
//        val expected = arrayOf(emojiStr)
//        val actual = InitializeBroadcastActivity().emojiStringToArray(emojiStr).toArray()
//        Assert.assertArrayEquals(expected, actual)
//    }
//
//    @Test
//    fun emojiStringToArray_singleCharEmojis_correctNumberEmojis() {
//        val str = "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\uD83E\uDD23\uD83D\uDE03\uD83D\uDE04\uD83D\uDE05\uD83D\uDE06"
//        val expected = arrayOf("😀", "😁","😂", "🤣", "😃", "😄", "😅", "😆")
//        val actual = InitializeBroadcastActivity().emojiStringToArray(str).toArray()
//        Assert.assertArrayEquals(expected, actual)
//    }
//
//    @Test
//    fun emojiStringToArray_singleCharMultipleCharEmojis_correctNumberEmojis() {
//        val str = "\uD83D\uDE00\uD83D\uDE01\uD83D\uDE02\uD83E\uDD23\uD83D\uDE03\uD83D\uDE04\uD83D\uDE05\uD83D\uDE06\uD83D\uDC69\uD83C\uDFFD\u200D\uD83C\uDFA8"
//        val expected = arrayOf("😀", "😁","😂", "🤣", "😃", "😄", "😅", "😆", "👩🏽‍🎨")
//        val actual = InitializeBroadcastActivity().emojiStringToArray(str).toArray()
//        Assert.assertEquals(expected.size, actual.size)
//
//    }
}