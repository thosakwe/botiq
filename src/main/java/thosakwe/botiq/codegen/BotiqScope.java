package thosakwe.botiq.codegen;

import org.antlr.v4.runtime.ParserRuleContext;
import thosakwe.botiq.codegen.data.BotiqDatum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotiqScope {
    private final BotiqToLlvmCompiler compiler;
    private Map<String, Integer> prefixes = new HashMap<String, Integer>();

    BotiqScope(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    private List<BotiqSymbol> getSymbols() {
        return symbols;
    }

    private List<BotiqSymbol> symbols = new ArrayList<BotiqSymbol>();

    private List<BotiqScope> getChildren() {
        return children;
    }

    private List<BotiqScope> children = new ArrayList<BotiqScope>();

    public BotiqSymbol createConstant(BotiqDatum value, String prefix, ParserRuleContext source) {
        BotiqSymbol result = new BotiqSymbol(compiler, prefix + incrementConstant(prefix));
        result.setValue(value, source);
        result.setConstant(true);
        symbols.add(result);
        return result;
    }

    private int incrementConstant(String prefix) {
        Integer level = prefixes.get(prefix);

        if (level == null) {
            prefixes.put(prefix, 0);
            return 0;
        } else {
            prefixes.put(prefix, ++level);
            return level;
        }
    }

    BotiqSymbol createSymbol(String id) {
        BotiqSymbol result = new BotiqSymbol(compiler, id);
        getCurrentScope().getSymbols().add(result);
        return result;
    }

    /**
     * Adds a new layer to the rootScope.
     */
    void enter() {
        children.add(new BotiqScope(compiler));
    }

    /**
     * Removes a layer from the rootScope.
     */
    void exit() {
        if (!children.isEmpty()) {
            children.remove(children.size() - 1);
        }
    }

    private BotiqScope getCurrentScope() {
        if (children.isEmpty())
            return this;

        return children.get(children.size() - 1);
    }

    BotiqDatum get(String id, ParserRuleContext source) {
        return get(id, source, true);
    }

    BotiqDatum get(String id, ParserRuleContext source, boolean throwIfAbsent) {
        BotiqSymbol symbol = getSymbol(id);

        if (symbol != null)
            return symbol.getValue();
        else {
            if (throwIfAbsent)
                compiler.error("The symbol '" + id + "' is undefined in this context.", source);
            return null;
        }
    }

    private BotiqSymbol getSymbol(String id) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BotiqScope scope = children.get(i);
            BotiqSymbol found = scope.getSymbol(id);

            if (found != null)
                return found;
        }

        for (BotiqSymbol symbol : symbols) {
            if (symbol.getId().equals(id))
                return symbol;
        }

        return null;
    }

    void put(String id, BotiqDatum value, ParserRuleContext source) {
        put(id, value, source, false);
    }

    void put(String id, BotiqDatum value, ParserRuleContext source, boolean constant) {
        BotiqSymbol target = getSymbol(id);

        if (target == null)
            target = createSymbol(id);

        target.setConstant(constant);
        target.setValue(value, source);
    }

    List<BotiqSymbol> getAllSymbols() {
        List<BotiqSymbol> result = new ArrayList<BotiqSymbol>();
        result.addAll(symbols);

        for (BotiqScope scope : getChildren()) {
            result.addAll(scope.getAllSymbols());
        }

        return result;
    }
}
