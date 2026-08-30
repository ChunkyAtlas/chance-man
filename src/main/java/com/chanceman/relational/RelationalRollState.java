package com.chanceman.relational;

/** Persisted counters that drive relational roll selection. */
public final class RelationalRollState
{
    private final long totalRolls;
    private final int rollsSinceToolRoll;
    private final int questMissStreak;

    public RelationalRollState()
    {
        this(0L, 0, 0);
    }

    public RelationalRollState(long totalRolls, int rollsSinceToolRoll, int questMissStreak)
    {
        this.totalRolls = Math.max(0L, totalRolls);
        this.rollsSinceToolRoll = Math.max(0, rollsSinceToolRoll);
        this.questMissStreak = Math.max(0, questMissStreak);
    }

    public long getTotalRolls()
    {
        return totalRolls;
    }

    public int getRollsSinceToolRoll()
    {
        return rollsSinceToolRoll;
    }

    public int getQuestMissStreak()
    {
        return questMissStreak;
    }

    public RelationalRollState afterRoll(boolean toolRoll, boolean questRoll)
    {
        return new RelationalRollState(
                totalRolls + 1,
                toolRoll ? 0 : rollsSinceToolRoll + 1,
                questRoll ? 0 : questMissStreak + 1
        );
    }
}
