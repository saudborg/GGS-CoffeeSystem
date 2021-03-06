package com.sauloborges.ggs.receiver;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sauloborges.ggs.constants.QueueConstants;
import com.sauloborges.ggs.domain.Programmer;
import com.sauloborges.ggs.domain.Statistic;

/**
 * This class represent the second machine in challenge. Where the programmer:
 * 
 * Pay the coffee they want at the cash register using cash or credit. This will
 * take 0.25 second for credit and 0.5 seconds for cash payments.
 * 
 * @author sauloborges
 *
 */
@Component
public class PayCoffeeReceiver {

	private static final Logger logger = LoggerFactory.getLogger(PayCoffeeReceiver.class);

	private CountDownLatch latch = new CountDownLatch(1);

	@Autowired
	RabbitTemplate rabbitTemplate;

	public void receiveMessage(Programmer programmer) throws InterruptedException {
		logger.debug("Received in pay queue:  <" + programmer.getName() + ">");
		// Set the time that the programmer leaves the queue and can pay
		programmer.setTimeLeavePayQueue(Calendar.getInstance().getTimeInMillis());

		// Get the payment method and how long it is takes
		Integer timeSpentByPaymenthMethod = programmer.getPaymenthMethod().getTime();
		Thread.sleep(timeSpentByPaymenthMethod);

		// Set the time that the programmer paid for your coffee and the time he
		// enters in a queue to get your coffee
		programmer.setTimePaid(Calendar.getInstance().getTimeInMillis());
		programmer.setTimeEnterGetTheCoffeeQueue(Calendar.getInstance().getTimeInMillis());
		rabbitTemplate.convertAndSend(QueueConstants.GET_COFFEE_IN_MACHINE_QUEUE, programmer);

		// send to queue to collect stats
		String machine = PayCoffeeReceiver.class.getName() + Thread.currentThread().getId();
		rabbitTemplate.convertAndSend(QueueConstants.STATISTICS_QUEUE, new Statistic(machine, programmer));

		logger.debug("Leaving pay queue:  <" + programmer.getName() + ">");

		latch.countDown();
	}

	public CountDownLatch getLatch() {
		return latch;
	}

}
