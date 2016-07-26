package thosakwe.botiq.codegen;

import thosakwe.botiq.antlr.BotiqParser;
import thosakwe.botiq.codegen.data.BotiqDatum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotiqScope {
    private final BotiqToLlvmCompiler compiler;
    private Map<String, Integer> prefixes = new HashMap<String, Integer>();

    public BotiqScope(BotiqToLlvmCompiler compiler) {
        this.compiler = compiler;
    }

    private List<BotiqSymbol> getSymbols() {
        return symbols;
    }

    private List<BotiqSymbol> symbols = new ArrayList<BotiqSymbol>();

    public List<BotiqScope> getChildren() {
        return children;
    }

    private List<BotiqScope> children = new ArrayList<BotiqScope>();

    public BotiqSymbol createConstant(BotiqDatum value, String prefix) {
        BotiqSymbol result = new BotiqSymbol(compiler, prefix + incrementConstant(prefix));
        result.setValue(value);
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

    BotiqScope getCurrentScope() {
        if (children.isEmpty())
            return this;

        return children.get(children.size() - 1);
    }

    BotiqDatum get(String id) {
        BotiqSymbol symbol = getSymbol(id);

        if (symbol != null)
        return getSymbol(id).getValue();
        else {
            compiler.error("The symbol '" + id + "' is undefined in this context.");
            return null;
        }
    }

    BotiqSymbol getSymbol(String id) {
        for (int i = children.size() - 1; i >= 0; i--) {
            BotiqScope scope = children.get(i);
            BotiqSymbol found = scope.getSymbol(id);

            if (found != null)
                return found;
        }

        for (BotiqSymbol symbol: symbols) {
            if (symbol.getId().equals(id))
                return symbol;
        }

        return null;
    }

    void put(String id, BotiqDatum value) {
        BotiqSymbol target = getSymbol(id);

        if (target == null)
            target = createSymbol(id);

        target.setValue(value);
    }

    List<BotiqSymbol> getAllSymbols() {
        List<BotiqSymbol> result = new ArrayList<BotiqSymbol>();
        result.addAll(symbols);

        for (BotiqScope scope: getChildren()) {
            result.addAll(scope.getAllSymbols());
        }

        return result;
    }
}
