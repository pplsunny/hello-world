package com.example.demo002.listener;

import com.rabbitmq.client.ConsumerCancelledException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.ListenerContainerConsumerFailedEvent;
import org.springframework.amqp.rabbit.listener.QueuesNotAvailableException;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RabbitMQListener implements ApplicationListener<ListenerContainerConsumerFailedEvent> {
    @Override
    public void onApplicationEvent(ListenerContainerConsumerFailedEvent event) {
        log.info("event:{}", event);
        SimpleMessageListenerContainer container = (SimpleMessageListenerContainer) event.getSource();
        Throwable exception = event.getThrowable();
        if (!event.isFatal()) {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (exception instanceof com.rabbitmq.client.ConsumerCancelledException || exception instanceof org.springframework.amqp.rabbit.support.ConsumerCancelledException) {
                log.error("event------ConsumerCancelledException:{}", event.getReason());
                boolean retryFlag = true;
                while (retryFlag) {
                    retryFlag = restartMulti(container, 5000);
                }
            }
            if (exception instanceof org.springframework.amqp.rabbit.listener.QueuesNotAvailableException) {
                log.info("event------org.springframework.amqp.rabbit.listener.QueuesNotAvailableException:{}", event.getReason());
                boolean retryFlag = true;
                while (retryFlag) {
                    retryFlag = restartMulti(container, 5000);
                }
            }

            if (exception instanceof com.rabbitmq.client.ShutdownSignalException) {
                log.info("event------com.rabbitmq.client.ShutdownSignalException:{}", event.getReason());
                restart(container);
            }
            if (exception instanceof org.springframework.amqp.AmqpConnectException) {
                log.info("event------org.springframework.amqp.AmqpConnectException:{}", event.getReason());

                //throw new QueuesNotAvailableException("RabbitMQ is not available", exception);
                restart(container);
            }
            //container.start();
        } else {
            if (exception instanceof org.springframework.amqp.rabbit.listener.QueuesNotAvailableException) {
                log.info("event------org.springframework.amqp.rabbit.listener.QueuesNotAvailableException:{}", event.getReason());
                boolean retryFlag = true;
                while (retryFlag) {
                    retryFlag = restartMulti(container, 5000);
                }
            }
        }

    }

    private void restart(SimpleMessageListenerContainer container) {
        container.start();
    }

    private boolean restartMulti(SimpleMessageListenerContainer container, long millis) {
        boolean retryFlag = true;
        try {
            log.info("RabbitMQ ConnectException, retrying-------------------");
            Thread.sleep(millis);
            container.start();
            retryFlag = false;

        }catch(org.springframework.amqp.AmqpIllegalStateException amqp){
            retryFlag = true;
        } catch (Exception e) {
            retryFlag = true;
        }
        return retryFlag;
    }
}
