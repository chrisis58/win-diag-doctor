package cn.teacy.wdd.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryContext {

    LogQueryRequest queryRequest;

    List<WinEventLogEntry> logEntries;

}
