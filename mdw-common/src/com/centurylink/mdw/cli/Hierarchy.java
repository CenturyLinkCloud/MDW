package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.model.workflow.Linked;
import com.centurylink.mdw.model.workflow.Process;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Parameters(commandNames="hierarchy", commandDescription="Process definition hierarchy", separators="=")
public class Hierarchy extends Setup {

    @Parameter(names="--process", description="Process for which heirarchy will be shown.", required=true)
    private String process;
    public String getProcess() {
        return process;
    }
    public void setProcess(String proc) {
        this.process = proc;
    }

    @Parameter(names="--max-depth", description="Maximum child depth (to avoid StackOverflowErrors)")
    private int maxDepth = 100;
    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    @Parameter(names="--downward", description="Only search downward from process")
    private boolean downward;
    public boolean isDownward() { return downward; }
    public void setDownward(boolean downward) { this.downward = downward; }


    private List<Linked<Process>> topLevelCallers = new ArrayList<>();
    public List<Linked<Process>> getTopLevelCallers() { return topLevelCallers; }

    private List<Process> processes;
    private Process start;

    Hierarchy() {

    }

    public Hierarchy(String process) {
        this.process = process;
    }

    public Hierarchy(Process process, List<Process> processes) {
        this.start = process;
        this.processes = processes;
    }

    @Override
    public Operation run(ProgressMonitor... monitors) throws IOException {
        for (ProgressMonitor monitor : monitors)
            monitor.progress(0);


        if (start == null) {
            if (!process.endsWith(".proc"))
                process += ".proc";
            start = loadProcess(getPackageName(process), getAssetFile(process), true);
        }

        for (ProgressMonitor monitor : monitors)
            monitor.progress(10);

        if (processes == null)
            loadProcesses(monitors);
        if (downward) {
            topLevelCallers.add(new Linked<>(start));
        }
        else {
            addTopLevelCallers(start);
        }

        for (int i = 0; i < topLevelCallers.size(); i++) {
            Linked<Process> topLevelCaller = topLevelCallers.get(i);
            addCalledHierarchy(topLevelCaller, 0);
            int prog =  (85 + ((int)Math.floor((double )(i * 10)/topLevelCallers.size())));
            for (ProgressMonitor monitor : monitors)
                monitor.progress(prog);
        }

        // weed out paths that don't contain starting process
        for (Linked<Process> topLevelCaller : topLevelCallers) {
            topLevelCaller.prune(start);
        }

        for (ProgressMonitor monitor : monitors)
            monitor.progress(100);

        if (isCommandLine()) {
            // print output
            getOut().println();
            print(topLevelCallers, 0);
        }

        return this;
    }

    private void print(List<Linked<Process>> callers, int depth) {
        for (Linked<Process> caller : callers) {
            print(caller, depth);
        }
    }

    private void print(Linked<Process> caller, int depth) {
        getOut().println(caller.toString(depth));
        print(caller.getChildren(), depth + 1);
    }

    public List<Process> findCallingProcesses(Process subproc) {
        List<Process> callers = new ArrayList<>();
        for (Process process : processes) {
            if (process.invokesSubprocess(subproc) && !callers.contains(process)) {
                callers.add(process);
            }
        }
        return callers;
    }

    public List<Process> findCalledProcesses(Process mainproc) {
        return mainproc.findInvoked(processes);
    }

    /**
     * Only loads current processes (not archived) that contain subprocs.
     * Starts at 10%, uses 80% monitor progress.
     */
    private void loadProcesses(ProgressMonitor... monitors) throws IOException {
        if (processes == null) {
            processes = new ArrayList<>();
            Map<String,List<File>> pkgProcFiles = findAllAssets("proc");
            for (String pkg : pkgProcFiles.keySet()) {
                List<File> procFiles = pkgProcFiles.get(pkg);
                for (int i = 0; i < procFiles.size(); i++) {
                    processes.add(loadProcess(pkg, procFiles.get(i), true));
                    int prog = 10 + ((int)Math.floor((double)(i * 80)/procFiles.size()));
                    for (ProgressMonitor monitor : monitors)
                        monitor.progress(prog);
                }
            }
        }
    }

    private void addTopLevelCallers(Process called) {
        List<Process> immediateCallers = findCallingProcesses(called);
        if (immediateCallers.isEmpty()) {
            topLevelCallers.add(new Linked<>(called));
        }
        else {
            for (Process caller : immediateCallers) {
                addTopLevelCallers(caller);
            }
        }
    }

    /**
     * Find subflow graph for caller.
     * @param caller - top level starting flow
     */
    private void addCalledHierarchy(Linked<Process> caller, int depth) throws IOException {
        depth++;
        Process callerProcess = caller.get();
        for (Process calledProcess : findCalledProcesses(callerProcess)) {
            Linked<Process> child = new Linked<>(calledProcess);
            child.setParent(caller);
            caller.getChildren().add(child);
            if (!child.checkCircular()) {
                if (depth > maxDepth) {
                    String message = "Allowable --max-depth (" + maxDepth + ") exceeded.";
                    getOut().println("\n" + message);
                    print(child.getCallChain(), 0);
                    throw new IOException(message + "  See tree output.");
                }
                else {
                    addCalledHierarchy(child, depth);
                }
            }
        }
    }
}
