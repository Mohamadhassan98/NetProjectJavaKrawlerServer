package net.project

import net.didion.jwnl.JWNL
import net.didion.jwnl.data.POS
import net.didion.jwnl.data.PointerType
import net.didion.jwnl.dictionary.Dictionary
import java.io.File
import java.io.FileInputStream

val dict: Dictionary by lazy {
    JWNL.initialize(FileInputStream(File("./lib/jwnl14-rc2/config/file_properties.xml")))
    Dictionary.getInstance()
}

fun String.getHyponyms() = dict.lookupIndexWord(POS.NOUN, this)
    ?.senses
    ?.flatMap {
        it.getPointers(PointerType.HYPONYM).flatMap {
            it.targetSynset.words.map {
                it.lemma
            }
        }
    }.orEmpty()
