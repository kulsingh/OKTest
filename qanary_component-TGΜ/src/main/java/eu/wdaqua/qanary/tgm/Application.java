package eu.wdaqua.qanary.tgm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.component.QanaryService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by didier on 27.03.16.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("eu.wdaqua.qanary.tgm")
public class Application {

	/**
	* this method is needed to make the QanaryComponent in this project known
	* to the QanaryServiceController in the qanary_component-template
	* 
	* @return
	*/
	@Bean
	public QanaryComponent qanaryComponent() {
		return new Tgm();
	}
	
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
