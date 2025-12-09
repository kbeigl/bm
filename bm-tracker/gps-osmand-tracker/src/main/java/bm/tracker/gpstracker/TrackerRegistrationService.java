package bm.tracker.gpstracker;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
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
public class TrackerRegistrationService implements ApplicationContextAware {
  private static final Logger logger = LoggerFactory.getLogger(TrackerRegistrationService.class);

  @Autowired private ProducerTemplate producer;
  @Autowired private CamelContext camel;
  @Autowired private AutowireCapableBeanFactory beanFactory;

  @Value("${osmand.host}")
  private String osmandHost;

  private ConfigurableApplicationContext configurableContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.configurableContext = (ConfigurableApplicationContext) applicationContext;
  }

  /** deviceId must match and creates the beanName "tracker-10" */
  public OsmAndTracker registerTracker(String deviceId) throws Exception {
    OsmAndTracker tracker = new OsmAndTracker(deviceId, osmandHost, camel, producer);
    beanFactory.autowireBean(tracker);
    String beanName = "tracker-" + deviceId;
    logger.info("Registering OsmAndTracker bean with name {} ", beanName);
    configurableContext.getBeanFactory().registerSingleton(beanName, tracker);
    return tracker;
  }
}
