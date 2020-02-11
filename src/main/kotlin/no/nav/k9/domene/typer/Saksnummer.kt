package no.nav.k9.domene.typer

import java.util.*
import java.util.regex.Pattern

class Saksnummer(saksnummer: String) {
    val verdi: String

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        } else if (obj == null || javaClass != obj.javaClass) {
            return false
        }
        val other = obj as Saksnummer
        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return Objects.hash(verdi)
    }

    override fun toString(): String {
        return javaClass.simpleName + "<" + verdi + ">"
    }

    companion object {
        private const val CHARS = "a-z0-9_:-"
        private val VALID = Pattern.compile(
            "^(-?[1-9]|[a-z0])[$CHARS]*$",
            Pattern.CASE_INSENSITIVE
        )
        private val INVALID = Pattern.compile(
            "[^$CHARS]+",
            Pattern.DOTALL or Pattern.CASE_INSENSITIVE
        )
    }

    init {
        Objects.requireNonNull(saksnummer, "saksnummer")
        require(VALID.matcher(saksnummer).matches()) {
            // skal ikke skje, funksjonelle feilmeldinger håndteres ikke her.
            "Ugyldig saksnummer, støtter kun A-Z/0-9/:/-/_ tegn. Var: " + saksnummer.replace(
                INVALID.pattern().toRegex(),
                "?"
            ) + " (vasket)"
        }
        verdi = saksnummer
    }
}