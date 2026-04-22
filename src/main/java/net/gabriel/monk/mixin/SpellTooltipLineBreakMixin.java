package net.gabriel.monk.mixin;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.spell_engine.client.gui.SpellTooltip;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;

@Mixin(SpellTooltip.class)
public class SpellTooltipLineBreakMixin {

    @Redirect(
            method = "addSpellDescription",
            at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z")
    )
    private static boolean monkmod$splitDescriptionLines(ArrayList<Text> lines, Object element) {
        if (!(element instanceof Text text)) {
            return lines.add((Text) element);
        }

        String full = text.getString();

        // Se não tem quebra de linha, deixa como o Spell Engine faz normalmente
        if (!full.contains("\n") && !full.contains("\r")) {
            return lines.add(text);
        }

        // Mantém cor/estilo que o Spell Engine aplicou
        Style style = text.getStyle();

        // Divide por qualquer tipo de quebra de linha (\n ou \r\n) e preserva linhas vazias (\n\n)
        String[] parts = full.split("\\R", -1);

        // Captura indentação (espaços iniciais) da primeira linha e reaplica nas demais
        String indent = leadingSpaces(parts.length > 0 ? parts[0] : "");

        for (String part : parts) {
            String cleaned = part;

            // evita duplicar indentação se já vier com os mesmos espaços
            if (cleaned.startsWith(indent)) {
                cleaned = cleaned.substring(indent.length());
            }

            MutableText newLine = Text.literal(indent + cleaned).setStyle(style);
            lines.add(newLine);
        }

        return true;
    }

    private static String leadingSpaces(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') break;
            i++;
        }
        return s.substring(0, i);
    }
}
