/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class AnEmptyController {
	//private VideoFileManager videoDataMgr;
	private static long currentId;
	//private Map<Long,Video> videos = new HashMap<Long, Video>();
	private ArrayList<Video> videos = new ArrayList<Video>();
	
	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos;
	}
	
	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		currentId = videos.isEmpty() ? 1 : currentId++;
		v.setId(currentId);
		v.setDataUrl(getDataUrl(currentId));
		videos.add(v);
		
		return v;
	}
	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@RequestBody Video v, @PathVariable("id") long id, @RequestParam("data") MultipartFile videoData, HttpServletResponse response){
		Video vid = getVideo(id);
		if(vid !=null){
			InputStream stream;
			try{
				stream = videoData.getInputStream();
				VideoFileManager.get().saveVideoData(vid, stream);
			} catch (IOException e){
				e.printStackTrace();
	            return null;
			}
		} else {
			response.setStatus(404);
			return null;
		}
		return new VideoStatus(VideoState.READY);	
	}
	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET)
	public @ResponseBody void getData(@PathVariable("id") long id, HttpServletResponse response) throws IOException {
		VideoFileManager videoDataMgr;
		try {
			videoDataMgr = VideoFileManager.get();
            serveSomeVideo(videoDataMgr,response, id);
		 } catch (IOException e) {
			 e.printStackTrace();
         }
	}
	
	// Initialize this member variable somewhere with 
    // videoDataMgr = VideoFileManager.get()
    //

  	
  	public void serveSomeVideo(VideoFileManager vDM, HttpServletResponse response, long id) throws IOException {
  	     // Of course, you would need to send some headers, etc. to the
  	     // client too!
  	     //  ...
        Video v = getVideo(id);
        if(v != null && vDM.hasVideoData(v)){
        	OutputStream out = response.getOutputStream();
        	vDM.copyVideoData(v, out);
        	response.setStatus(200);
        } else{
        	response.setStatus(404);
        	return;
        }
  	}
	/*
	private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
        }
	 }*/
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

	private Video getVideo(long id){
		try{
			return videos.get((int) (id - 1));
		} catch(ArrayIndexOutOfBoundsException ex){
			return null;
		}
	}
	
 	private String getUrlBaseForLocalServer() {
	   HttpServletRequest request = 
	       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
	   String base = 
	      "http://"+request.getServerName() 
	      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
	   return base;
	}

}
