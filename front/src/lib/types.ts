export type JwtResponse = {
  token: string;
  type: string;
  username: string;
  roles: string[];
};

export type PlaylistDTO = {
  id?: number;
  name: string;
  description?: string;
  isPublic: boolean;
  creatorId?: number;
  songIds: number[];
};

export type SongDTO = {
  id: number;
  title: string;
  duration_ms: number;
  genre?: string;
  bpm?: number;
  picture_url?: string;
  artistIds: number[];
  albumId?: number;
};

export type AlbumDTO = {
  id: number;
  name: string;
  releaseDate?: string;
  pictureUrl?: string;
  artistId?: number;
  songIds: number[];
};

export type ArtistDTO = {
  id: number;
  name: string;
  biography?: string;
  photoUrl?: string;
  albumIds: number[];
  songIds: number[];
};

export type User = {
  id: number;
  username: string;
  profile?: unknown;
};

export type SignupRequest = {
  username: string;
  password: string;
  isAdmin?: boolean;
};
