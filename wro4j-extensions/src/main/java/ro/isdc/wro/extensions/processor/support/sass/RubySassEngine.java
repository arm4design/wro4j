package ro.isdc.wro.extensions.processor.support.sass;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.WroRuntimeException;


/**
 * A Sass processor using ruby gems.
 *
 * @author Dmitry Erman
 * @created 12 Feb 2012
 * @since 1.4.4
 */
public class RubySassEngine {
  private static final Logger LOG = LoggerFactory.getLogger(RubySassEngine.class);
  private static final String RUBY_GEM_REQUIRE = "rubygems";
  private static final String SASS_PLUGIN_REQUIRE = "sass/plugin";
  private static final String SASS_ENGINE_REQUIRE = "sass/engine";

  private final Set<String> requires;

  public RubySassEngine() {
    System.setProperty("org.jruby.embed.compat.version", "JRuby1.9");
    requires = new LinkedHashSet<String>();
    requires.add(RUBY_GEM_REQUIRE);
    requires.add(SASS_PLUGIN_REQUIRE);
    requires.add(SASS_ENGINE_REQUIRE);
  }

  /**
   * Adds a ruby require to the ruby script to be run by this RubySassEngine. It's safe to add the same require twice.
   *
   * @param require
   *          The name of the require, e.g. bourbon
   */
  public void addRequire(final String require) {
    if (require != null && require.trim().length() > 0) {
      requires.add(require.trim());
    }
  }

  /**
   * Transforms a sass content into css using Sass ruby engine. This method is synchronized because the engine itself is
   * not thread-safe.
   *
   * @param content
   *          the Sass content to process.
   */
  public synchronized String process(final String content) {
    if (StringUtils.isEmpty(content)) {
      return StringUtils.EMPTY;
    }
    try {
      final ScriptEngine rubyEngine = new ScriptEngineManager().getEngineByName("jruby");
      return rubyEngine.eval(buildUpdateScript(content)).toString();
    } catch (final ScriptException e) {
      throw new WroRuntimeException(e.getMessage(), e);
    }
  }

  private String buildUpdateScript(final String content) {
    Validate.notNull(content);
    final StringWriter raw = new StringWriter();
    final PrintWriter script = new PrintWriter(raw);
    final StringBuilder sb = new StringBuilder();
    final StringBuilder cb = new StringBuilder();
    sb.append(":syntax => :scss");

    for (final String require : requires) {
      script.println("  require '" + require + "'                                   ");
    }
    final int BACKSLASH = 0x5c;
    for (int i = 0; i < content.length(); i++) {
      final int code = content.codePointAt(i);
      if (code < 0x80) {
        // We leave only ASCII unchanged.
        if (code == BACKSLASH) {
          // escape backslash
          cb.append("\\");
        }
        cb.append(content.charAt(i));
      } else {
        // Non-ASCII String may cause invalid multibyte char (US-ASCII) error with Ruby 1.9
        // because Ruby 1.9 expects you to use ASCII characters in your source code.
        // Instead we use Unicode code point representation which is usable with
        // Ruby 1.9 and later.
        cb.append(String.format("\\u%04x", code));
      }
    }
    final String scriptAsString = String.format("result = Sass::Engine.new(\"%s\", {%s}).render",
        cb.toString().replace("\"", "\\\"").replace("#", "\\#"), // escape ", #
        sb.toString());
    LOG.debug("scriptAsString: {}", scriptAsString);
    script.println(scriptAsString);
    script.flush();
    return raw.toString();
  }
}
