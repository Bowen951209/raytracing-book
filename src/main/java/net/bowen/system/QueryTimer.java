package net.bowen.system;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL42.*;

public class QueryTimer extends Deleteable{

    private final int queryID;
    private final int[] timeElapsed;

    private boolean resultAvailable;

    public QueryTimer() {
        super(true);
        queryID = glGenQueries();
        timeElapsed = new int[1];
    }

    public void startQuery() {
        glBeginQuery(GL_TIME_ELAPSED, queryID);
    }

    public void endQuery() {
        glEndQuery(GL_TIME_ELAPSED);
        resultAvailable = false; // Reset flag
    }

    public boolean checkResultAvailable() {
        if (!resultAvailable) {
            int[] queryStatus = new int[1];
            glGetQueryObjectiv(queryID, GL_QUERY_RESULT_AVAILABLE, queryStatus);
            resultAvailable = (queryStatus[0] == GL_TRUE);
        }
        return resultAvailable;
    }

    public int getElapsedTime() {
        if (resultAvailable) {
            glGetQueryObjectiv(queryID, GL_QUERY_RESULT, timeElapsed);
            // Convert nanoseconds to milliseconds
            return (int) (timeElapsed[0] / 1_000_000.0);
        } else {
            throw new IllegalStateException("Query result is not available yet.");
        }
    }

    @Override
    protected void delete() {
        glDeleteQueries(queryID);
        System.out.println("Query object(" + queryID + ") deleted.");
    }
}
