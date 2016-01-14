package com.sauloborges.ggs.receiver;

import static com.sauloborges.ggs.constants.TimeConstants.FINDING_A_CUP;
import static com.sauloborges.ggs.constants.TimeConstants.GETTING_THE_CUP;
import static com.sauloborges.ggs.constants.TimeConstants.PICKING_THE_TYPE;
import static com.sauloborges.ggs.constants.TimeConstants.PUTTING_INTO_OUTLET;
import static com.sauloborges.ggs.util.Utils.calculateTimeSpentInCoffeeMachine;
import static com.sauloborges.ggs.util.Utils.calculateTimeSpentInMachineCofffeQueue;
import static com.sauloborges.ggs.util.Utils.calculateTimeSpentInPayQueue;
import static com.sauloborges.ggs.util.Utils.calculateTimeSpentToPaidAndGotACoffee;
import static com.sauloborges.ggs.util.Utils.calculateTotalTimeSpent;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sauloborges.ggs.ExternalConfigComponent;
import com.sauloborges.ggs.constants.QueueConstants;
import com.sauloborges.ggs.domain.Programmer;
import com.sauloborges.ggs.domain.Statistic;
import com.sauloborges.ggs.repository.ProgrammerRepository;

@Component
public class GetCoffeeInMachineReceiver {

	private static final Logger logger = LoggerFactory.getLogger(GetCoffeeInMachineReceiver.class);

	private CountDownLatch latch = new CountDownLatch(1);

	@Autowired
	RabbitTemplate rabbitTemplate;

	@Autowired
	ExternalConfigComponent externalConfigComponent;

	@Autowired
	private ProgrammerRepository programmerRepository;

	public void receiveMessage(Programmer programmer) throws InterruptedException {
		logger.debug("Received in a machine to get a coffee: <" + programmer.getName() + ">");
		programmer.setTimeLeaveGetTheCoffeeQueue(Calendar.getInstance().getTimeInMillis());
		programmer.setTimeStartedToGetTheCoffe(Calendar.getInstance().getTimeInMillis());

		Thread.sleep(FINDING_A_CUP);
		Thread.sleep(PUTTING_INTO_OUTLET);
		Thread.sleep(PICKING_THE_TYPE);

		Integer timeByTypeOfCoffe = programmer.getCoffeType().getTime(); // PREPARING
		Thread.sleep(timeByTypeOfCoffe);

		Thread.sleep(GETTING_THE_CUP);

		programmer.setTimeFinished(Calendar.getInstance().getTimeInMillis());

		// send to queue to collect stats
		String machine = GetCoffeeInMachineReceiver.class.getName() + Thread.currentThread().getId();
		rabbitTemplate.convertAndSend(QueueConstants.STATISTICS_QUEUE, new Statistic(machine, programmer));

		// update the times
		programmerRepository.save(programmer);

		latch.countDown();
		prettyPrint(programmer);

	}

	private void prettyPrint(Programmer programmer) {
		StringBuffer sb = new StringBuffer();
		sb.append("\nUser <" + programmer.getName() + "> got your coffee. ");
		sb.append("\n\tSpent total: " + calculateTotalTimeSpent(programmer));
		sb.append("\n\tSpent in the pay queue: " + calculateTimeSpentInPayQueue(programmer));
		sb.append("\n\tSpent to paid and got a coffee: " + calculateTimeSpentToPaidAndGotACoffee(programmer));
		sb.append("\n\tSpent in the machine coffee queue: " + calculateTimeSpentInMachineCofffeQueue(programmer));
		sb.append("\n\tSpent to got the coffee in machine: " + calculateTimeSpentInCoffeeMachine(programmer));
		logger.info(sb.toString());

		if (externalConfigComponent.showDetailsInConsole())
			System.out.println(sb.toString());
	}

}
