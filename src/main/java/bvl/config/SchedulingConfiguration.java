package bvl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Planificador del sondeo.
 *
 * <p>Un solo hilo a proposito: los sondeos no deben solaparse, porque cada uno escribe en la misma
 * base y en los mismos ficheros XLS. Se declara explicitamente en lugar de depender del que
 * autoconfigura Boot, que solo aparece con {@code @EnableScheduling}, y aqui la tarea se programa
 * a mano desde {@code BvlScheduler}.
 */
@Configuration
public class SchedulingConfiguration {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("bvl-sondeo-");
        // Que un cierre de la aplicacion no deje un sondeo a medias escribiendo el XLS.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
