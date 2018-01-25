package com.test.li182.my_game;

import android.support.annotation.NonNull;

/**
 * Created by li182 on 2018/1/20.
 */

public class Player implements Comparable<Player>{
    String name;
    boolean ready;
    boolean ok = false;
    long score = -1;
    int touID ;
    String address;

    public Player(String name,int touID,String address) {

        this.name = name;
        ready = false;
        ok = false;
        score = -1;
        this.touID = touID;
        this.address = address;
    }

    public int getTouID() {
        return touID;
    }

    public void setTouID(int touID) {
        this.touID = touID;
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
