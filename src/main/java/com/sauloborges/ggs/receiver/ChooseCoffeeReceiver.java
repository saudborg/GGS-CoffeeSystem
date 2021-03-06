package com.sauloborges.ggs.receiver;

import static com.sauloborges.ggs.constants.TimeConstants.CHOOSING_TYPE_COFFEE;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sauloborges.ggs.constants.QueueConstants;
import com.sauloborges.ggs.domain.CoffeeType;
import com.sauloborges.ggs.domain.Programmer;
import com.sauloborges.ggs.domain.Statistic;
import com.sauloborges.ggs.repository.ProgrammerRepository;

/**
 * This class represent the first machine in challenge. Where the programmer:
 * 
 * First gather information regarding which types of coffee are available
 * (espresso, latte macchiato or cappuccino) and pick their personal favorite.
 * This will take 0.5 seconds.
 * 
 * @author sauloborges
 *
 */
@Component
public class ChooseCoffeeReceiver {

	private static final Logger logger = LoggerFactory.getLogger(ChooseCoffeeReceiver.class);

	private CountDownLatch latch = new CountDownLatch(1);

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	private ProgrammerRepository programmerRepository;

	public void receiveMessage(Programmer programmer) throws InterruptedException {
		logger.debug("Received in choose queue:  <" + programmer.getName() + ">");
		programmer.setTimeStarted(Calendar.getInstance().getTimeInMillis());

		Thread.sleep(CHOOSING_TYPE_COFFEE);
		programmer.setCoffeType(CoffeeType.getARandomCoffe());

		programmer.setTimeEnterPayQueue(Calendar.getInstance().getTimeInMillis());

		// save programmer after record you time that he will enter in a queue
		Programmer saved = programmerRepository.save(programmer);
		logger.debug("Programmer saved id: " + saved.getId());
		programmer = saved;

		// Send to a queue to pay for you coffee
		rabbitTemplate.convertAndSend(QueueConstants.PAY_COFFEE_QUEUE, programmer);

		// send to a queue to collect stats
		String machine = ChooseCoffeeReceiver.class.getName() + Thread.currentThread().getId();
		rabbitTemplate.convertAndSend(QueueConstants.STATISTICS_QUEUE, new Statistic(machine, programmer));

		logger.debug("Leaving in choose queue:  <" + programmer.getName() + "> / CoffeType = "
				+ programmer.getCoffeType().getType());
		latch.countDown();
	}

}
