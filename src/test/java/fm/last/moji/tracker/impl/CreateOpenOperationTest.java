/*
 * Copyright 2012 Last.fm
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package fm.last.moji.tracker.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import fm.last.moji.tracker.Destination;
import fm.last.moji.tracker.TrackerException;

@RunWith(MockitoJUnitRunner.class)
public class CreateOpenOperationTest {

  private static final String DOMAIN = "domain";
  private static final String STORAGE_CLASS = "storageClass";
  private static final String KEY = "key";
  private static final String FID = "5";
  private static final String DEV_COUNT = "2";
  private static final String PATH_1 = "http://www.last.fm/1/";
  private static final String DEV_ID_1 = "1";
  private static final String PATH_2 = "http://www.last.fm/2/";
  private static final String DEV_ID_2 = "2";

  @Mock
  private RequestHandler mockRequestHandler;
  @Mock
  private Response mockResponse;
  @Mock
  private Response mockEmptyResponse;
  @Mock
  private Response mockUnknownKeyResponse;
  @Mock
  private Response mockFailResponse;

  private ArgumentCaptor<Request> captorRequest;
  private CreateOpenOperation operation;

  @Before
  public void setUp() throws TrackerException {
    captorRequest = ArgumentCaptor.forClass(Request.class);
    when(mockResponse.getStatus()).thenReturn(ResponseStatus.OK);
    when(mockResponse.getValue("fid")).thenReturn(FID);
    when(mockResponse.getValue("dev_count")).thenReturn(DEV_COUNT);
    when(mockResponse.getValue("path_1")).thenReturn(PATH_1);
    when(mockResponse.getValue("devid_1")).thenReturn(DEV_ID_1);
    when(mockResponse.getValue("path_2")).thenReturn(PATH_2);
    when(mockResponse.getValue("devid_2")).thenReturn(DEV_ID_2);

    when(mockEmptyResponse.getStatus()).thenReturn(ResponseStatus.OK);
    when(mockEmptyResponse.getValue("fid")).thenReturn(FID);
    when(mockEmptyResponse.getValue("dev_count")).thenReturn("0");

    when(mockUnknownKeyResponse.getStatus()).thenReturn(ResponseStatus.ERROR);
    when(mockUnknownKeyResponse.getMessage()).thenReturn("unknown_key unknown key");

    when(mockFailResponse.getStatus()).thenReturn(ResponseStatus.ERROR);
    when(mockFailResponse.getMessage()).thenReturn("unexpected error");
  }

  @Test
  public void requestNormal() throws TrackerException, MalformedURLException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, STORAGE_CLASS, true);
    operation.execute();

    Request request = captorRequest.getValue();
    assertThat(request.getCommand(), is("create_open"));
    assertThat(request.getArguments().size(), is(4));
    assertThat(request.getArguments().get("domain"), is(DOMAIN));
    assertThat(request.getArguments().get("class"), is(STORAGE_CLASS));
    assertThat(request.getArguments().get("key"), is(KEY));
    assertThat(request.getArguments().get("multi_dest"), is("1"));
  }

  @Test
  public void requestNormalNoClass() throws TrackerException, MalformedURLException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, null, true);
    operation.execute();

    Request request = captorRequest.getValue();
    assertThat(request.getCommand(), is("create_open"));
    assertThat(request.getArguments().size(), is(3));
    assertThat(request.getArguments().get("domain"), is(DOMAIN));
    assertThat(request.getArguments().get("key"), is(KEY));
    assertThat(request.getArguments().get("multi_dest"), is("1"));
  }

  @Test
  public void requestNormalNoMulti() throws TrackerException, MalformedURLException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, "", false);
    operation.execute();

    Request request = captorRequest.getValue();
    assertThat(request.getCommand(), is("create_open"));
    assertThat(request.getArguments().size(), is(3));
    assertThat(request.getArguments().get("domain"), is(DOMAIN));
    assertThat(request.getArguments().get("key"), is(KEY));
    assertThat(request.getArguments().get("multi_dest"), is("0"));
  }

  @Test
  public void responseNormal() throws TrackerException, MalformedURLException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, STORAGE_CLASS, true);
    operation.execute();

    List<Destination> destinations = operation.getDestinations();
    assertThat(destinations.size(), is(2));

    Destination destination1 = destinations.get(0);
    assertThat(destination1.getDevId(), is(1));
    assertThat(destination1.getFid(), is(5));
    assertThat(destination1.getPath(), is(new URL(PATH_1)));

    Destination destination2 = destinations.get(1);
    assertThat(destination2.getDevId(), is(2));
    assertThat(destination2.getFid(), is(5));
    assertThat(destination2.getPath(), is(new URL(PATH_2)));
  }

  @Test
  public void zeroDestinations() throws TrackerException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockEmptyResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, STORAGE_CLASS, true);
    operation.execute();

    List<Destination> destinations = operation.getDestinations();
    assertThat(destinations.size(), is(0));
  }

  @Test
  public void unknownKey() throws TrackerException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockUnknownKeyResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, STORAGE_CLASS, true);
    operation.execute();

    List<Destination> destinations = operation.getDestinations();
    assertThat(destinations.size(), is(0));
  }

  @Test(expected = TrackerException.class)
  public void unexpectedError() throws TrackerException {
    when(mockRequestHandler.performRequest(captorRequest.capture())).thenReturn(mockFailResponse);

    operation = new CreateOpenOperation(mockRequestHandler, DOMAIN, KEY, STORAGE_CLASS, true);
    operation.execute();
  }

}
