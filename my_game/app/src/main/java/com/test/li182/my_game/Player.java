package com.test.li182.my_game;

import android.support.annotation.NonNull;

import java.util.Comparator;

/**
 * Created by li182 on 2018/1/20.
 */

public class Player implements Comparable<Player>{
    String name;
    boolean ready;
    boolean ok = false;
    long score = -1;
    String address;

    public Player(String name,String address) {

        this.name = name;
        ready = false;
        ok = false;
        score = -1;
        this.address = address;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }


    @Override
    public int compareTo(@NonNull Player o) {
        return (this.score>o.score)?-1:1;
    }
}
