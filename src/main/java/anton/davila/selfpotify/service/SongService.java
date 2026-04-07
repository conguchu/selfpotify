package anton.davila.selfpotify.service;

import anton.davila.selfpotify.entity.music.Album;
import anton.davila.selfpotify.entity.music.Song;
import anton.davila.selfpotify.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;


    // =====================================
    // ----- CRUD
    // =====================================
    public Song add(Song s) {
        log.info("Intentando añadir una nueva canción: {}", s.getTitle());
        Song saved = songRepository.save(s);
        log.debug("Canción guardada con éxito. ID: {}", saved.getId());
        return saved;
    }
    public List<Song> getAll() {
        log.info("Recuperando todas las canciones de la base de datos");
        List<Song> songs =  songRepository.findAll();
        log.debug("Se han encontrado {} canciones", songs.size());
        return songs;
    }
    public Optional<Song> getById(long id) {
        log.info("Buscando canción por ID: {}", id);
        return songRepository.findById(id);
    }
    @Transactional
    public Song update(long id, Song songData) {
        log.info("Iniciando actualización de la canción con ID: {}", id);
        Optional<Song> s = getById(id);
        if (s.isEmpty()) {
            log.error("Error al actualizar: No se encontró la canción con ID {}", id);
            throw new RuntimeException("No se ha encontrado la cancion con ID " + id);
        }
        Song song = s.get();
        song.copy(songData);
        log.info("Canción con ID {} actualizada correctamente en el contexto de persistencia", id);
        return song;

        // (metodo para copiar los atributos del objeto sin tener que hacerlo a mano siempre)
    }
    public Song delete(long id) {
        log.warn("Intentando eliminar la canción con ID: {}", id);
        Optional<Song> s = getById(id);
        if (s.isEmpty()) {
            log.error("Error al eliminar: No existe la canción con ID {}", id);
            throw new RuntimeException("No se ha encontrado la cancion con ID " + id);
        }
        Song song = s.get();
        songRepository.delete(song);
        log.info("Canción con ID {} eliminada correctamente", id);
        return song;
    }


    public List<Song> saveMany(List<Song> songs) {
        log.warn("Guardando una lista de " + songs.size() + " canciones...");
        songRepository.saveAll(songs);
        log.info("Guardadas " + songs.size() + " canciones correctamente.");
        return songs;
    }

    @Transactional
    public List<Song> loadFolder(String folderPath) {
        log.info("Iniciando escaneo de carpeta: {}", folderPath);
        List<Song> songsToSave = new ArrayList<>();

        // recorre la carpeta filtrando archivos mp3 y wav
        try
                (Stream<Path> paths = Files.walk(Paths.get(folderPath)))
        {
            List<Path> audioFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.toString().toLowerCase();
                        return fileName.endsWith(".mp3") || fileName.endsWith(".wav");
                    })
                    .toList();

            log.info("Se encontraron {} archivos de audio", audioFiles.size());

            // crea la lista de canciones extrayendo los metadatos
            for (Path filePath : audioFiles) {
                Song song = extractMetadata(filePath.toFile());
                songsToSave.add(song);
            }

        } catch (IOException e) {
            log.error("Error al leer el directorio: {}", folderPath, e);
            throw new RuntimeException("No se pudo acceder a la ruta especificada", e);
        }

        // persiste la lista de canciones y la devuelve
        log.info("Persistiendo {} canciones en la base de datos...", songsToSave.size());
        return saveMany(songsToSave);
    }

    /**
     * Métºdo auxiliar para extraer los metadatos de un archivo físico y mapearlo a la entidad Song.
     */
    private Song extractMetadata(File file) {
        Song song = new Song();
        song.setSongPath(file.getAbsolutePath());
        song.setListeners(0); // Valor por defecto para canciones nuevas

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            // Extracción de datos del Header (información técnica del audio)
            if (header != null) {
                // getTrackLength() devuelve segundos. Lo pasamos a ms según tu entidad.
                song.setDuration_ms(header.getTrackLength() * 1000);
            }

            // extracción de datos del Tag (metadatos ID3)
            if (tag != null) {
                // titulo
                // si no hay metadato, usamos el nombre del archivo como fallback
                String title = tag.getFirst(FieldKey.TITLE);
                song.setTitle((title != null && !title.isBlank()) ? title : file.getName());

                // género
                song.setGenre(tag.getFirst(FieldKey.GENRE));

                // BPM
                String bpmStr = tag.getFirst(FieldKey.BPM);
                if (bpmStr != null && !bpmStr.isBlank()) {
                    try {
                        song.setBpm(Integer.parseInt(bpmStr));
                    } catch (NumberFormatException e) {
                        log.debug("El formato BPM del archivo {} no es un número válido: {}", file.getName(), bpmStr);
                    }
                }
            } else {
                // si no hay tags en absoluto, aseguramos que al menos tenga un título
                song.setTitle(file.getName());
            }

        } catch (Exception e) {
            // capturamos cualquier excepción de JAudioTagger para que un archivo corrupto no detenga el lote completo
            log.warn("No se pudieron extraer todos los metadatos del archivo: {}. Usando datos básicos.", file.getName());
            song.setTitle(file.getName());
        }

        return song;
    }
}


