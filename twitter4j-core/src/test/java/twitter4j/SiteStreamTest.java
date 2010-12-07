/*
Copyright (c) 2007-2010, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package twitter4j;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;
import twitter4j.conf.PropertyConfiguration;

import java.io.InputStream;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.1.8
 */
public class SiteStreamTest extends TwitterTestBase implements SiteStreamListener {
    public SiteStreamTest(String name) {
        super(name);
    }

    public void testStream() throws Exception {
        InputStream is = SiteStreamTest.class.getResourceAsStream("/sitestream-testcase.json");
        SiteStreamImpl siteStream = new SiteStreamImpl(ConfigurationContext.getInstance(), is);
        SiteStreamListener[] listeners = new SiteStreamListener[1];
        listeners[0] = this;
        received.clear();
        siteStream.next(listeners);
        assertEquals(6358482, received.get(0)[0]);
        received.clear();
        siteStream.next(listeners);
        assertEquals(new Integer(6358481), received.get(0)[0]);
        received.clear();
        siteStream.next(listeners);
        assertEquals(new Integer(4933401), received.get(0)[0]);
    }

    public void testSiteStream() throws Exception {
        InputStream is = SiteStreamTest.class.getResourceAsStream("/sitestream-test.properties");
        if (null == is) {
            System.out.println("sitestream-test.properties not found. skipping Site Streams test.");
        } else {
            Properties props = new Properties();
            props.load(is);
            is.close();
            Configuration yusukeyConf = new PropertyConfiguration(props, "/yusukey");
            Configuration twit4jConf = new PropertyConfiguration(props, "/twit4j");
            Configuration twit4j2Conf = new PropertyConfiguration(props, "/twit4j2");
            TwitterStream twitterStream = new TwitterStreamFactory(yusukeyConf).getInstance();
            twitterStream.addListener(this);
            Twitter twit4j = new TwitterFactory(twit4jConf).getInstance();
            Twitter twit4j2 = new TwitterFactory(twit4j2Conf).getInstance();
            try {
                twit4j.destroyBlock(6377362);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j2.destroyBlock(6358482);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j.createFriendship(6377362);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j2.createFriendship(6358482);
            } catch (TwitterException ignore) {
            }

            //twit4j: 6358482
            //twit4j2: 6377362
            twitterStream.site(true, new int[]{6377362, 6358482});
            //expecting onFriendList for twit4j and twit4j2
            waitForStatus();
            waitForStatus();

            Status status = twit4j2.updateStatus("@twit4j " + new Date());
            //expecting onStatus for twit4j from twit4j
            waitForStatus();
            assertReceived("onstatus");
            assertReceived("onfriendlist");

            twit4j.createFavorite(status.getId());
            waitForStatus();

            twit4j.destroyFavorite(status.getId());
            waitForStatus();

            // unfollow twit4j
            twit4j2.destroyFriendship(6358482);
            waitForStatus();

            // follow twit4j
            twit4j2.createFriendship(6358482);
            waitForStatus();

            // unfollow twit4j2
            twit4j.destroyFriendship(6377362);
            waitForStatus();

            // follow twit4j2
            twit4j.createFriendship(6377362);
            waitForStatus();

            twit4j.retweetStatus(status.getId());
            twit4j.sendDirectMessage(6377362, "test " + new Date());

            // block twit4j
            twit4j2.createBlock(6358482);
            waitForStatus();

            // unblock twit4j
            twit4j2.destroyBlock(6358482);
            waitForStatus();

            try {
                twit4j.createFriendship(6377362);
            } catch (TwitterException ignore) {
            }
            try {
                twit4j2.createFriendship(6358482);
            } catch (TwitterException ignore) {
            }

            UserList list = twit4j.createUserList("test", true, "desctription");
            waitForStatus();
            list = twit4j.updateUserList(list.getId(),"test2",true,"description2");
            waitForStatus();
            twit4j.addUserListMember(list.getId(), 6377362);
            twit4j2.subscribeUserList("twit4j", list.getId());
            waitForStatus();
            twit4j2.unsubscribeUserList("twit4j", list.getId());
            waitForStatus();
            twit4j.destroyUserList(list.getId());
            waitForStatus();

            assertReceived(TwitterMethod.CREATE_FAVORITE);
            assertReceived(TwitterMethod.DESTROY_FAVORITE);
//            assertReceived(TwitterMethod.DESTROY_FRIENDSHIP);
            assertReceived(TwitterMethod.CREATE_FRIENDSHIP);
//            assertReceived(TwitterMethod.RETWEET_STATUS);
            assertReceived(TwitterMethod.SEND_DIRECT_MESSAGE);

            assertReceived(TwitterMethod.SUBSCRIBE_LIST);
            assertReceived(TwitterMethod.CREATE_USER_LIST);
            assertReceived(TwitterMethod.UPDATE_USER_LIST);
            assertReceived(TwitterMethod.DESTROY_USER_LIST);


            assertReceived(TwitterMethod.CREATE_BLOCK);
            assertReceived(TwitterMethod.DESTROY_BLOCK);
        }
    }

    private void assertReceived(Object obj) {
        boolean received = false;
        for (Object[] event : this.received) {
            if (obj.equals(event[0])) {
                received = true;
                break;
            }
        }
        assertTrue(received);
    }

//    public void testSiteStreamPull() throws Exception {
//        InputStream is = SiteStreamTest.class.getResourceAsStream("/sitestream-test.properties");
//        if (null == is) {
//            System.out.println("sitestream-test.properties not found. skipping Site Streams test.");
//        } else {
//            Properties props = new Properties();
//            props.load(is);
//            is.close();
//            Configuration conf = new PropertyConfiguration(props,"/yusukey");
//            TwitterStream twitterStream = new TwitterStreamFactory(conf).getInstance();
//            is = twitterStream.getSiteStream(true, new int[]{4933401, 6358482});
//            InputStreamReader isr = new InputStreamReader(is);
//            BufferedReader br = new BufferedReader(isr);
//            String line;
//            while (null != (line = br.readLine())) {
//                System.out.println(line);
//            }
//        }
//    }

    private synchronized void waitForStatus() {
        try {
            this.wait(5000);
            System.out.println("notified.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    List<Object[]> received = new ArrayList<Object[]>(3);

    private synchronized void notifyResponse() {
        this.notify();
    }

    public void onStatus(int forUser, Status status) {
        received.add(new Object[]{"onstatus", forUser, status});
        notifyResponse();
    }

    public void onFriendList(int forUser, int[] friendIds) {
        received.add(new Object[]{"onfriendlist", forUser, friendIds});
        notifyResponse();
    }

    public void onFavorite(int forUser, User source, User target, Status targetObject) {
        received.add(new Object[]{TwitterMethod.CREATE_FAVORITE, forUser, source, target, targetObject});
        notifyResponse();
    }

    public void onUnfavorite(int forUser, User source, User target, Status targetObject) {
        received.add(new Object[]{TwitterMethod.DESTROY_FAVORITE, forUser, source, target, targetObject});
        notifyResponse();
    }

    public void onFollow(int forUser, User source, User target) {
        received.add(new Object[]{TwitterMethod.CREATE_FRIENDSHIP, forUser, source, target});
        notifyResponse();
    }

    public void onDirectMessage(int forUser, DirectMessage directMessage) {
        received.add(new Object[]{TwitterMethod.SEND_DIRECT_MESSAGE, forUser, directMessage});
        notifyResponse();
    }

    public void onUserListSubscribed(int forUser, User subscriber, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.SUBSCRIBE_LIST, forUser, subscriber, listOwner, list});
        notifyResponse();
    }

    public void onUserListCreated(int forUser, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.CREATE_USER_LIST, forUser, listOwner, list});
        notifyResponse();
    }

    public void onUserListUpdated(int forUser, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.UPDATE_USER_LIST, forUser, listOwner, list});
        notifyResponse();
    }

    public void onUserListDestroyed(int forUser, User listOwner, UserList list) {
        received.add(new Object[]{TwitterMethod.DESTROY_USER_LIST, forUser, listOwner, list});
        notifyResponse();
    }

    public void onBlock(int forUser, User source, User target) {
        received.add(new Object[]{TwitterMethod.CREATE_BLOCK, forUser, source, target});
        notifyResponse();
    }

    public void onUnblock(int forUser, User source, User target) {
        received.add(new Object[]{TwitterMethod.DESTROY_BLOCK, forUser, source, target});
        notifyResponse();
    }

    public void onException(Exception ex) {
        received.add(new Object[]{ex});
        notifyResponse();
    }
}
