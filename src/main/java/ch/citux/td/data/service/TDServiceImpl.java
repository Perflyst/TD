/*
 * Copyright 2013-2014 Paul Stöhr
 *
 * This file is part of TD.
 *
 * TD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.citux.td.data.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.Playlist;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.citux.td.R;
import ch.citux.td.config.TDConfig;
import ch.citux.td.data.dto.JustinArchive;
import ch.citux.td.data.dto.TwitchAccessToken;
import ch.citux.td.data.dto.TwitchChannel;
import ch.citux.td.data.dto.TwitchChannels;
import ch.citux.td.data.dto.TwitchFollows;
import ch.citux.td.data.dto.TwitchStream;
import ch.citux.td.data.dto.TwitchVideos;
import ch.citux.td.data.model.Channel;
import ch.citux.td.data.model.Follows;
import ch.citux.td.data.model.Response;
import ch.citux.td.data.model.SearchChannels;
import ch.citux.td.data.model.SearchStreams;
import ch.citux.td.data.model.Stream;
import ch.citux.td.data.model.StreamPlayList;
import ch.citux.td.data.model.StreamQuality;
import ch.citux.td.data.model.StreamToken;
import ch.citux.td.data.model.VideoPlaylist;
import ch.citux.td.data.model.Videos;
import ch.citux.td.data.worker.TDRequestHandler;
import ch.citux.td.util.DtoMapper;
import ch.citux.td.util.Log;

public class TDServiceImpl implements TDService {

    private static final String TAG = "TDService";

    private static TDServiceImpl instance;

    private Gson gson;
    private Gson jGson;

    private TDServiceImpl() {
        gson = new Gson();
        jGson = new GsonBuilder().setDateFormat("y-M-d H:m:s 'UTC'").create(); //2013-01-19 02:49:36 UTC
    }

    public static TDServiceImpl getInstance() {
        if (instance == null) {
            instance = new TDServiceImpl();
        }
        return instance;
    }

    private String buildUrl(String base, Object... params) {
        ArrayList<String> args = new ArrayList<String>(params.length);

        for (Object param : params) {
            try {
                String arg = param.toString();
                arg = URLEncoder.encode(arg, TDConfig.UTF_8);
                args.add(arg);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e);
            } catch (NullPointerException e) {
                Log.e(TAG, e);
            }
        }
        return MessageFormat.format(base, args.toArray());
    }

    @Override
    public Follows getFollows(String username) {
        Follows result = new Follows();
        String url = buildUrl(TDConfig.URL_API_GET_FOLLOWS, username);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchFollows twitchFollows = gson.fromJson(response.getResult(), TwitchFollows.class);
            if (twitchFollows != null && twitchFollows.getFollows() != null) {
                result.setChannels(DtoMapper.mapTwitchChannels(twitchFollows.getFollows()));
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public Channel getChannel(String channel) {
        Channel result = new Channel();
        String url = buildUrl(TDConfig.URL_API_GET_CHANNEL, channel);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchChannel twitchChannel = gson.fromJson(response.getResult(), TwitchChannel.class);
            if (twitchChannel != null) {
                result = DtoMapper.mapChannel(twitchChannel);
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public Stream getStream(String channel) {
        Stream result = new Stream();
        String url = buildUrl(TDConfig.URL_API_GET_STREAM, channel);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchStream twitchStream = gson.fromJson(response.getResult(), TwitchStream.class);
            if (twitchStream != null && (twitchStream.getStream() != null || twitchStream.getStreams() != null)) {
                result = DtoMapper.mapStream(twitchStream.getStream());
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public Videos getVideos(String channel) {
        Videos result = new Videos();
        String url = buildUrl(TDConfig.URL_API_GET_VIDEOS, channel, 10);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchVideos videos = gson.fromJson(response.getResult(), TwitchVideos.class);
            if (videos != null) {
                result.setVideos(DtoMapper.mapVideos(videos));
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public VideoPlaylist getVideoPlaylist(String id) {
        VideoPlaylist result = new VideoPlaylist();
        String url = buildUrl(TDConfig.URL_API_GET_VIDEO, id);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            List<JustinArchive> archives = jGson.fromJson(response.getResult(), new TypeToken<List<JustinArchive>>() {
            }.getType());
            if (archives != null) {
                result = (DtoMapper.mapVideo(archives));
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public StreamToken getStreamToken(String channel) {
        StreamToken result = new StreamToken();
        String url = buildUrl(TDConfig.URL_API_GET_STREAM_TOKEN, channel);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchAccessToken accessToken = gson.fromJson(response.getResult(), TwitchAccessToken.class);
            if (accessToken != null && accessToken.getToken() != null && accessToken.getSig() != null) {
                result.setNauth(accessToken.getToken());
                result.setNauthsig(accessToken.getSig());
                result.setP((int) (Math.random() * 999999));
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public StreamPlayList getStreamPlaylist(String channel, StreamToken streamToken) {
        StreamPlayList result = new StreamPlayList();
        String url = buildUrl(TDConfig.URL_API_GET_STREAM_PLAYLIST, channel, streamToken.getP(), streamToken.getNauth(), streamToken.getNauthsig());
        Response<Playlist> response = TDRequestHandler.startPlaylistRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            List<Element> elements = response.getResult().getElements();
            if (elements.size() > 0) {
                HashMap<StreamQuality, String> streams = new HashMap<StreamQuality, String>();
                for (Element element : elements) {
                    Log.d(TAG, "URI: " + element.getURI() + " Name: " + element.getName());
                    StreamQuality quality = StreamPlayList.parseQuality(element.getName());
                    if (quality != null) {
                        streams.put(quality, element.getURI().toString());
                    }
                }
                result.setStreams(streams);
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public SearchStreams searchStreams(String query, String offset) {
        SearchStreams result = new SearchStreams();
        String url = buildUrl(TDConfig.URL_API_SEARCH_STREAMS, query, offset);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchStream searchStreams = jGson.fromJson(response.getResult(), TwitchStream.class);
            if (searchStreams != null) {
                result = (DtoMapper.mapSearchStreams(searchStreams));
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }

    @Override
    public SearchChannels searchChannels(String query, String offset) {
        SearchChannels result = new SearchChannels();
        String url = buildUrl(TDConfig.URL_API_SEARCH_CHANNELS, query, offset);
        Response<String> response = TDRequestHandler.startStringRequest(url);
        if (response.getStatus() == Response.Status.OK) {
            TwitchChannels searchStreams = jGson.fromJson(response.getResult(), TwitchChannels.class);
            if (searchStreams != null) {
                result = (DtoMapper.mapSearchChannels(searchStreams));
            }
        } else {
            result.setErrorResId(R.string.error_data_error_message);
        }
        return result;
    }
}