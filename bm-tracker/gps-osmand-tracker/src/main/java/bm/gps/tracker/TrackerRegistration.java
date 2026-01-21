package bm.gps.tracker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class TrackerRegistration implements ApplicationContextAware {
  private static final Logger logger = LoggerFactory.getLogger(TrackerRegistration.class);

  @Value("${osmand.host}")
  private String osmandHost;

  // @Autowired private AutowireCapableBeanFactory beanFactory;
  private ConfigurableApplicationContext configurableContext;
  // per-bean locks to avoid coarse-grained synchronization
  private final ConcurrentMap<String, Object> registrationLocks = new ConcurrentHashMap<>();

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.configurableContext = (ConfigurableApplicationContext) applicationContext;
  }

  /**
   * Creation and Registration of a TrackerOsmAnd instance. If Tracker already exists, return
   * existing.
   */
  public TrackerOsmAnd registerTracker(String uniqueId) { // throws Exception {
    String beanName = "tracker-" + uniqueId;
    logger.info("Registering OsmAnd Tracker bean with name {} ", beanName);

    Object lock = registrationLocks.computeIfAbsent(beanName, k -> new Object());
    try {
      synchronized (lock) {
        // first quick check if bean already exists
        try {
          Object existingBean = configurableContext.getBean(beanName);
          logger.warn("bean {} is already registered: {} ", beanName, existingBean.toString());
          if (existingBean instanceof TrackerOsmAnd) {
            return (TrackerOsmAnd) existingBean;
          } else {
            logger.warn(
                "Existing bean {} is not a TrackerOsmAnd, type={}",
                beanName,
                existingBean.getClass());
            return null;
          }
        } catch (BeansException ex) {
          // expected if bean not found - proceed to register
        }

        // Register a BeanDefinition so Spring manages full lifecycle
        // (constructor, @PostConstruct, @PreDestroy)
        DefaultListableBeanFactory dlbf =
            (DefaultListableBeanFactory) configurableContext.getBeanFactory();
        try {
          GenericBeanDefinition bd = new GenericBeanDefinition();
          bd.setBeanClass(TrackerOsmAnd.class);
          bd.setScope(BeanDefinition.SCOPE_SINGLETON);
          // constructor args: uniqueId, osmandHost
          ConstructorArgumentValues cav = new ConstructorArgumentValues();
          cav.addIndexedArgumentValue(0, uniqueId);
          cav.addIndexedArgumentValue(1, osmandHost);
          bd.setConstructorArgumentValues(cav);

          dlbf.registerBeanDefinition(beanName, bd);
        } catch (BeanDefinitionStoreException bdse) {
          // another thread registered concurrently; return the existing bean
          logger.warn(
              "Race detected or bean definition exists for {}: {}", beanName, bdse.getMessage());
          try {
            Object existing = configurableContext.getBean(beanName);
            if (existing instanceof TrackerOsmAnd) return (TrackerOsmAnd) existing;
            else {
              logger.warn(
                  "Existing bean {} is not a TrackerOsmAnd after race, type={}",
                  beanName,
                  existing.getClass());
              return null;
            }
          } catch (BeansException ex) {
            throw new RuntimeException("Failed to retrieve bean after concurrent registration", ex);
          }
        }

        // Retrieve the fully-initialized bean from the context (triggers creation)
        try {
          Object created = configurableContext.getBean(beanName);
          if (created instanceof TrackerOsmAnd) return (TrackerOsmAnd) created;
          else {
            logger.warn(
                "Registered bean {} is not TrackerOsmAnd, type={}", beanName, created.getClass());
            return null;
          }
        } catch (BeansException ex) {
          throw new RuntimeException("Failed to obtain created tracker bean " + beanName, ex);
        }
      }
    } finally {
      // cleanup lock to avoid memory leak; remove only if still mapped to our lock
      registrationLocks.remove(beanName, lock);
    }
  }

  /** Lookup an existing tracker singleton by uniqueId. Returns null if not present. */
  public TrackerOsmAnd lookupTracker(String uniqueId) {
    if (uniqueId == null) return null;
    String beanName = "tracker-" + uniqueId;
    try {
      if (configurableContext.containsBean(beanName)) {
        Object bean = configurableContext.getBean(beanName);
        if (bean instanceof TrackerOsmAnd) {
          return (TrackerOsmAnd) bean;
        }
      }
    } catch (BeansException be) {
      logger.debug("lookupTracker: bean {} not available: {}", beanName, be.getMessage());
    }
    return null;
  }

  /**
   * Unregisters a tracker previously registered with registerTracker. Destroys the singleton
   * instance (triggers @PreDestroy when possible) and removes the bean definition. Returns true if
   * any bean/definition was removed.
   */
  public boolean unregisterTracker(String uniqueId) {
    if (uniqueId == null) return false;
    String beanName = "tracker-" + uniqueId;
    try {
      DefaultListableBeanFactory dlbf =
          (DefaultListableBeanFactory) configurableContext.getBeanFactory();

      // If a singleton instance exists, try to invoke explicit cleanup and destroy it
      if (dlbf.containsSingleton(beanName)) {
        try {
          Object bean = null;
          try {
            bean = configurableContext.getBean(beanName);
          } catch (BeansException ignored) {
          }
          if (bean != null) {
            try {
              java.lang.reflect.Method m = bean.getClass().getDeclaredMethod("destroyRoutes");
              m.setAccessible(true);
              m.invoke(bean);
              logger.info("Invoked destroyRoutes on {} before singleton destroy", beanName);
            } catch (NoSuchMethodException ignored) {
              // no explicit destroyRoutes, rely on @PreDestroy if available
            }
          }
        } catch (Exception e) {
          logger.warn("Failed to invoke destroyRoutes for {}: {}", beanName, e.getMessage());
        }

        try {
          dlbf.destroySingleton(beanName);
          logger.info("Destroyed tracker singleton {}", beanName);
        } catch (Exception e) {
          logger.warn("Failed to destroy singleton {}: {}", beanName, e.getMessage());
        }
      }

      // Remove bean definition so it can be re-registered cleanly
      try {
        if (dlbf.containsBeanDefinition(beanName)) {
          dlbf.removeBeanDefinition(beanName);
          logger.info("Removed bean definition {}", beanName);
        }
      } catch (Exception e) {
        logger.warn("Failed to remove bean definition {}: {}", beanName, e.getMessage());
      }

      return true;
    } catch (Exception e) {
      logger.warn("unregisterTracker failed for {}: {}", uniqueId, e.getMessage());
      return false;
    }
  }
}
