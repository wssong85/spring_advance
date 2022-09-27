package hello.advanced.trace.logtrace;

import hello.advanced.trace.TraceId;
import hello.advanced.trace.TraceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FiledLogTrace implements LogTrace{

  private static final String START_PREFIX = "-->";
  private static final String COMPLETE_PREFIX = "<--";
  private static final String EX_PREFIX = "<X-";

  private TraceId traceIdHolder; // traceId 동기화, 동시성 이슈 발생

  @Override
  public TraceStatus begin(String message) {
    syncTraceId();
    TraceId traceId = traceIdHolder;
    Long startTimeMs = System.currentTimeMillis();
    log.info("[{}] {}{}", traceId.getId(), addSpace(START_PREFIX, traceId.getLevel()), message);
    return new TraceStatus(traceId, startTimeMs, message);
  }

  private void syncTraceId() {
    if (traceIdHolder == null) {
      traceIdHolder = new TraceId();
    } else {
      traceIdHolder = traceIdHolder.createNextId();
    }
  }

  public TraceStatus beginSync(TraceId beforeTraceId, String message) {
    TraceId nextId = beforeTraceId.createNextId();
    Long startTimeMs = System.currentTimeMillis();
    log.info("[{}] {}{}", nextId.getId(), addSpace(START_PREFIX, nextId.getLevel()), message);
    return new TraceStatus(nextId, startTimeMs, message);
  }

  @Override
  public void end(TraceStatus status) {
    complete(status, null);
  }

  @Override
  public void exception(TraceStatus status, Exception e) {
    complete(status, e);
  }

  private void complete(TraceStatus status, Exception e) {
    Long stopTimeMs = System.currentTimeMillis();
    long resultTimeMs = stopTimeMs - status.getStartTimeMs();
    TraceId traceId = status.getTraceId();
    if (e == null) {
      log.info("[{}] {}{} time={}ms", traceId.getId(),
          addSpace(COMPLETE_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs);
    } else {
      log.info("[{}] {}{} time={}ms ex={}", traceId.getId(),
          addSpace(EX_PREFIX, traceId.getLevel()), status.getMessage(), resultTimeMs,
          e.toString());
    }

    releaseTraceId();
  }

  private void releaseTraceId() {
    if (traceIdHolder.isFirstLevel()) {
      traceIdHolder = null; // destroy
    } else {
      traceIdHolder = traceIdHolder.createPreviousId();
    }
  }

  private static String addSpace(String prefix, int level) {

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < level; i++) {

      // level 마지막 전까지 | 로 구분자를 주다가 마지막에 | + prefix 를 넣어준다.
//      if (i == level - 1) {
//        sb.append("|" + prefix);
//      } else {
//        sb.append("| ");
//      }
      sb.append((i == level - 1) ? "|" + prefix : "| ");
    }

    return sb.toString();
  }
}
