package bm.gps.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class TrackerRegistration implements ApplicationContextAware {
  private static final Logger logger = LoggerFactory.getLogger(TrackerRegistration.class);

  @Autowired private AutowireCapableBeanFactory beanFactory;

  @Value("${osmand.host}")
  private String osmandHost;

  private ConfigurableApplicationContext configurableContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.configurableContext = (ConfigurableApplicationContext) applicationContext;
  }

  /** Creation and Registration of a TrackerOsmAnd instance. */
  public TrackerOsmAnd registerTracker(String uniqueId) throws Exception {

    // check if uniqueId is already registered

    TrackerOsmAnd tracker = new TrackerOsmAnd(uniqueId, osmandHost);
    // dependency injection
    beanFactory.autowireBean(tracker);
    String beanName = "tracker-" + uniqueId;
    logger.info("Registering OsmAnd Tracker bean with name {} ", beanName);
    // register singleton so the bean is visible in the context
    configurableContext.getBeanFactory().registerSingleton(beanName, tracker);
    // initialize bean to trigger lifecycle callbacks (e.g. @PostConstruct)
    beanFactory.initializeBean(tracker, beanName);
    return tracker;
  }
}
