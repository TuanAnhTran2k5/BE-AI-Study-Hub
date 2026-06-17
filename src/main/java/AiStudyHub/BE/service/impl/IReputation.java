package AiStudyHub.BE.service.impl;

public interface IReputation {
    int runDailyReputation();
    boolean applyReputation(Long documentId);
    boolean recomputeAllAggregates();
}
