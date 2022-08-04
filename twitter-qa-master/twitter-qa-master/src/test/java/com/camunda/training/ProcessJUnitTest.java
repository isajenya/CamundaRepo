package com.camunda.training;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.junit5.test.ProcessEngineExtension;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {

    @Test
    @Deployment(resources = "twitter-qa.bpmn")
    public void testHappyPath() {

        Map<String, Object> variables = new HashMap<String, Object>();
        Random random = new Random();
        variables.put("content", "EX4_IS_" + random.nextInt());
        ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

        assertThat(processInstance).isWaitingAt(findId("Review tweet"));
        List<Task> taskList = taskService()
                .createTaskQuery()
                .taskCandidateGroup("management")
                .processInstanceId(processInstance.getId())
                .list();

        assertThat(taskList).isNotNull();
        assertThat(taskList).hasSize(1);

        Task task = taskList.get(0);

        Map<String, Object> approvedMap = new HashMap<String, Object>();
        approvedMap.put("approved", true);
        taskService().complete(task.getId(), approvedMap);


        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();
        assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);
        execute(job);

        assertThat(processInstance).isNotNull();
        assertThat(processInstance).isEnded();
    }


    @Test
    @Deployment(resources = "twitter-qa.bpmn")
    public void testTweetRejected() {
        Map<String, Object> varMap = new HashMap<String, Object>();
        Random random = new Random();
        varMap.put("content", "EX8_IS_" + random.nextInt());
        varMap.put("approved", false);

        ProcessInstance processInstance = runtimeService().
                createProcessInstanceByKey("TwitterQAProcess")
                .setVariables(varMap)
                .startAfterActivity("review_tweet")
                .execute();

        assertThat(processInstance)
                .isWaitingAt(findId("Notify user of rejection"))
                .externalTask()
                .hasTopicName("notification");
        complete(externalTask());

        assertThat(processInstance).isEnded().hasPassed(findId("Tweet rejected"));


    }


    @Test
    @Deployment(resources = "twitter-qa.bpmn")
    public void testSuperUserTweet() {
        ProcessInstance processInstance = runtimeService()
                .createMessageCorrelation("superuserTweet")
                .setVariable("content", "ISJ11 " + System.currentTimeMillis())
                .correlateWithResult()
                .getProcessInstance();

        assertThat(processInstance).isStarted();

        /*runtimeService()
                .createMessageCorrelation("tweetWithdrawn")
                .correlateWithResult();*/

        List<Job> jobList = jobQuery()
                .processInstanceId(processInstance.getId())
                .list();

        assertThat(jobList).hasSize(1);
        Job job = jobList.get(0);
        execute(job);
        assertThat(processInstance).isEnded();

    }

    @Test
    @Deployment(resources = "twitter-qa.bpmn")
    public void testTweetWithdrawn() {

        Map<String, Object> varMap = new HashMap<>();

        varMap.put("content", "Test tweetWithdrawn message");

        ProcessInstance processInstance = runtimeService()
                .startProcessInstanceByKey("TwitterQAProcess", varMap);
        assertThat(processInstance).isStarted().isWaitingAt(findId("Review tweet"));

        runtimeService()
                .createMessageCorrelation("tweetWithdrawn")
                .processInstanceVariableEquals("content", "Test tweetWithdrawn message")
                .correlateWithResult();

        assertThat(processInstance).isEnded();
    }


    /*@Test
    @Deployment(resources = "tweetApproval.dmn")
    public void testTweetFromJakob() {
        Map<String, Object> variables = withVariables("email", "jakob.freund@camunda.com", "content", "this should be published");
        DmnDecisionTableResult decisionResult = decisionService().evaluateDecisionTableByKey("tweetApproval", variables);

        assertThat(decisionResult.getFirstResult()).contains(entry("approved", true));

    }*/


}
