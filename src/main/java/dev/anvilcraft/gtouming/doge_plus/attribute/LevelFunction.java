package dev.anvilcraft.gtouming.doge_plus.attribute;

/**
 * 等级函数：把「等级 → 数值」的规律用表达式表达，避免把整张表逐个写死。
 *
 * <p>支持 {@code + - * / % ^}、括号、一元正负号、数字与变量 {@code level}，
 * 例如 {@code "2 * level"}、{@code "level - 1"}、{@code "level ^ 2"}。</p>
 */
public final class LevelFunction {

    private final String source;
    private final Node root;

    private LevelFunction(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    /** 解析表达式；语法错误抛 {@link IllegalArgumentException}。 */
    public static LevelFunction parse(String source) {
        Parser parser = new Parser(source);
        Node root = parser.parseExpression();
        parser.expectEnd();
        return new LevelFunction(source, root);
    }

    /** 求该等级下的数值。 */
    public double evaluate(int level) {
        return root.eval(level);
    }

    public String source() {
        return source;
    }

    @Override
    public String toString() {
        return source;
    }

    // ==================== 内部实现 ====================

    private interface Node {
        double eval(double level);
    }

    private record Num(double value) implements Node {
        @Override
        public double eval(double level) {
            return value;
        }
    }

    private record Var() implements Node {
        @Override
        public double eval(double level) {
            return level;
        }
    }

    private record Neg(Node child) implements Node {
        @Override
        public double eval(double level) {
            return -child.eval(level);
        }
    }

    private record Bin(char op, Node left, Node right) implements Node {
        @Override
        public double eval(double level) {
            double a = left.eval(level);
            double b = right.eval(level);
            return switch (op) {
                case '+' -> a + b;
                case '-' -> a - b;
                case '*' -> a * b;
                case '/' -> a / b;
                case '%' -> a % b;
                case '^' -> Math.pow(a, b);
                default -> throw new IllegalStateException("Unexpected operator: " + op);
            };
        }
    }

    private static final class Parser {
        private final String source;
        private int pos;

        Parser(String source) {
            this.source = source;
        }

        Node parseExpression() {
            Node node = parseTerm();
            while (true) {
                skipSpace();
                char c = current();
                if (c == '+' || c == '-') {
                    pos++;
                    node = new Bin(c, node, parseTerm());
                } else {
                    return node;
                }
            }
        }

        Node parseTerm() {
            Node node = parsePower();
            while (true) {
                skipSpace();
                char c = current();
                if (c == '*' || c == '/' || c == '%') {
                    pos++;
                    node = new Bin(c, node, parsePower());
                } else {
                    return node;
                }
            }
        }

        Node parsePower() {
            Node base = parseUnary();
            skipSpace();
            if (current() == '^') {
                pos++;
                return new Bin('^', base, parsePower());
            }
            return base;
        }

        Node parseUnary() {
            skipSpace();
            char c = current();
            if (c == '-') {
                pos++;
                return new Neg(parseUnary());
            }
            if (c == '+') {
                pos++;
                return parseUnary();
            }
            return parseAtom();
        }

        Node parseAtom() {
            skipSpace();
            char c = current();
            if (c == '(') {
                pos++;
                Node node = parseExpression();
                skipSpace();
                expect(')');
                return node;
            }
            if (Character.isDigit(c)) {
                return new Num(parseNumber());
            }
            if (Character.isLetter(c)) {
                String id = parseIdentifier();
                if (id.equals("level")) return new Var();
                throw new IllegalArgumentException("Unknown identifier: " + id + " in: " + source);
            }
            throw new IllegalArgumentException("Unexpected character '" + c + "' at " + pos + " in: " + source);
        }

        double parseNumber() {
            int start = pos;
            while (pos < source.length()
                    && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                pos++;
            }
            return Double.parseDouble(source.substring(start, pos));
        }

        String parseIdentifier() {
            int start = pos;
            while (pos < source.length() && Character.isLetterOrDigit(source.charAt(pos))) {
                pos++;
            }
            return source.substring(start, pos);
        }

        void expect(char c) {
            skipSpace();
            if (current() != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at " + pos + " in: " + source);
            }
            pos++;
        }

        void expectEnd() {
            skipSpace();
            if (pos != source.length()) {
                throw new IllegalArgumentException("Trailing characters at " + pos + " in: " + source);
            }
        }

        private void skipSpace() {
            while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
                pos++;
            }
        }

        private char current() {
            return pos < source.length() ? source.charAt(pos) : '\0';
        }
    }
}
