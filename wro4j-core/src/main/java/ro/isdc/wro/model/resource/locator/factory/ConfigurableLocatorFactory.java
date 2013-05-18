/**
 * Copyright@2011 wro4j
 */
package ro.isdc.wro.model.resource.locator.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.model.resource.locator.ResourceLocator;
import ro.isdc.wro.model.resource.locator.support.LocatorProvider;
import ro.isdc.wro.model.resource.processor.factory.ProcessorsFactory;
import ro.isdc.wro.model.resource.support.AbstractConfigurableMultipleStrategy;


/**
 * A {@link ProcessorsFactory} implementation which is easy to configure using a {@link Properties} object.
 *
 * @author Alex Objelean
 * @created 30 Jul 2011
 * @since 1.4.0
 */
public class ConfigurableLocatorFactory
    extends AbstractConfigurableMultipleStrategy<ResourceLocatorFactory, LocatorProvider>
    implements ResourceLocatorFactory {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigurableLocatorFactory.class);
  /**
   * Name of init param used to specify uri locators.
   */
  public static final String PARAM_URI_LOCATORS = "uriLocators";

  private final ResourceLocatorFactory locatorFactory = newLocatorFactory();

  /**
   * {@inheritDoc}
   */
  @Override
  protected String getStrategyKey() {
    return PARAM_URI_LOCATORS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Map<String, ResourceLocatorFactory> getStrategies(final LocatorProvider provider) {
    return provider.provideLocators();
  }

  /**
   * {@inheritDoc}
   */
  public ResourceLocator getLocator(final String uri) {
    return locatorFactory.getLocator(uri);
  }

  /**
   * {@inheritDoc}
   */
  public InputStream locate(final String uri)
      throws IOException {
    return new AutoCloseInputStream(locatorFactory.locate(uri));
  }

  /**
   * {@inheritDoc}
   */
  private ResourceLocatorFactory newLocatorFactory() {
    final SimpleResourceLocatorFactory factory = new SimpleResourceLocatorFactory();
    final List<ResourceLocatorFactory> locatorFactories = getConfiguredStrategies();
    for (final ResourceLocatorFactory locatorFactory : locatorFactories) {
      factory.addFactory(locatorFactory);
    }
    // use default when none provided
    if (locatorFactories.isEmpty()) {
      LOG.debug("No locators configured. Using Default locator factory.");
      return new DefaultResourceLocatorFactory();
    }
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  public ResourceLocator getInstance(final String uri) {
    return locatorFactory.getLocator(uri);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Class<LocatorProvider> getProviderClass() {
    return LocatorProvider.class;
  }
}
