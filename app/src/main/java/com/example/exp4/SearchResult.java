package com.example.exp4;

import java.util.ArrayList;

public class SearchResult {
    private Result result;

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    static class Result {
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
            private ArrayList<Artist> artists = new ArrayList<>();

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

            public ArrayList<Artist> getArtists() {
                return artists;
            }

            public void setArtists(ArrayList<Artist> artists) {
                this.artists = artists;
            }

            static class Artist {
                private String name;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
        }
    }
}