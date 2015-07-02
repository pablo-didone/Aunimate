package mubbi.aunimate.model;


public class Sound {
    private String id;
    private String autor;
    private String title;

    public Sound(String id, String autor, String title) {
        this.id = id;
        this.autor = autor;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}