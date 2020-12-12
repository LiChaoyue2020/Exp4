package com.example.exp4;

import java.util.ArrayList;

public class SongDetail {

    private ArrayList<Song> songs = new ArrayList<>();

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    static class Song {
        private int id;
        private String name;
        private Al al;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Al getAl() {
            return al;
        }

        public void setAl(Al al) {
            this.al = al;
        }

        static class Al {
            private String picUrl;

            public String getPicUrl() {
                return picUrl;
            }

            public void setPicUrl(String picUrl) {
                this.picUrl = picUrl;
            }
        }
    }
}

