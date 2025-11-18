package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.entity.WinEventLogEntry;

import java.util.List;

public interface IWinEventLogCleaner {

    List<WinEventLogEntry> handle(List<WinEventLogEntry> logEntries);

}
