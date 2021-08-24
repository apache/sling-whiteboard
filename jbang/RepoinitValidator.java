///usr/bin/env jbang "$0" "$@" ; exit $? 
//DEPS org.apache.sling:org.apache.sling.repoinit.parser:1.6.10
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.apache.sling.repoinit.parser.operations.Operation;
import java.io.Reader;
import java.io.InputStreamReader;
import java.util.List;

/** To call this directly from GitHub with https://www.jbang.dev/ :
 *
 *      jbang trust add https://github.com/apache/sling-whiteboard/
 *      jbang https://github.com/apache/sling-whiteboard/blob/master/jbang/RepoinitValidator.java < some-repoinit-script.txt
 *
 */
class RepoinitValidator {

    public static void main(String[] args) {
        final RepoInitParserService parser = new RepoInitParserService();
        try (Reader input = new InputStreamReader(System.in, "UTF-8")) {
            final List<Operation> ops = parser.parse(input);
            System.out.println("Repoinit parsing successful:");
            System.out.println(ops);
        } catch(Exception e) {
            System.err.println("Exception: " + e);
        }
    }
}