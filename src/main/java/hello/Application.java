package hello;

import java.lang.invoke.MethodHandles;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.NodeConfig.NodeConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	static Application instance;
	static CoreContainer cores;

	public Application() {
		log.info("Application initialized");
		instance = this;
		this.initialize();
	}
	
	public void initialize() {
		Path path = FileSystems.getDefault().getPath("hello");
		ClassLoader parent = this.getClass().getClassLoader();
		NodeConfig nc = new NodeConfigBuilder("node",new SolrResourceLoader(path , parent))
				.build();
		cores = new CoreContainer(nc);
		log.info("We will search config within: " + cores.getSolrHome());
		cores.load();
		log.info("security.json:" + Paths.get(cores.getSolrHome()).resolve("security.json"));
		log.info ("Authentication plugin is:" + cores.getAuthenticationPlugin());
		log.info ("updateShardHalder:" + cores.getUpdateShardHandler());
		log.info ("updateShardHalder.HttpClient:" + cores.getUpdateShardHandler().getHttpClient());
		log.info("ShardHandlerFactory: " + cores.getShardHandlerFactory());
		//initializeAuthenticationPlugin();
	}
	
	public static Application getInstance() {
		return(instance);
	}
	public CoreContainer getCoreContainer() {
		return(cores);
	}
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    //@Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

        };
    }

}
