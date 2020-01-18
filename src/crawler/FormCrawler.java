package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import net.project.UtilsKt;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import utils.StaticAttributes;

import java.io.FileWriter;
import java.util.*;
import java.util.regex.Pattern;


public class FormCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz))$");

    private final boolean external;
    private final Set<String> data;
    private final String baseUrl;

    public FormCrawler(boolean external, Set<String> data, String baseUrl) {
        this.data = data;
        this.external = external;
        this.baseUrl = baseUrl;
    }

    public static void form(String htmlText) {
        Document html = Jsoup.parse(htmlText);
        try {
            Elements form = html.getElementsByTag("form");
            List<FormElement> forms = form.forms();

            for (int i = 0; i < forms.size(); i++) {
                String action = forms.get(i).attributes().get("action");
                String id = form.get(i).id();
                String method = forms.get(i).attributes().get("method");
                Set<Element> elementList = new LinkedHashSet<>(forms.get(i).getElementsByTag("input"));



                // for select random word from wordNetLists and try to submiting
                for (int k = 0; k < 10; k++) {
                    List<Connection.KeyVal> inputs = forms.get(i).formData();
                    for (int j = 0; j < inputs.size(); j++) {
                        String key = inputs.get(j).key();
                        String type = "";
                        Element elementTemp = (Element) elementList.toArray()[j];
                        if(elementTemp.attr("name").equals(key))
                            type = elementTemp.attr("type");
                        Element eTemp = forms.get(i)
                                .selectFirst("#" + key);
                        String wordTemp;
                        System.out.println(type);
                        switch (type) {
                            case "text":
                            case "search":
                                wordTemp = UtilsKt.getRandomHyponym(inputs.get(j).key());
                                eTemp.val(wordTemp.isEmpty() ? "abs" : wordTemp);
                                break;
                            case "date":
                                eTemp.val(StaticAttributes.randomDate.get(k));
                                break;
                            case "email":
                                eTemp.val(StaticAttributes.randomEmail.get(k));
                                break;
                            case "month":
                                eTemp.val(StaticAttributes.randomMonth.get(k));
                                break;
                            case "number":
                                eTemp.val(new Random().nextInt()+"");
                                break;
                            case "tel":
                                eTemp.val(StaticAttributes.randomTel.get(k));
                                break;
                            case "time":
                                eTemp.val(StaticAttributes.randomTime.get(k));
                                break;
                            case "week":
                                eTemp.val(StaticAttributes.randomWeek.get(k));
                                break;
                            default:
                                eTemp.val("haaale");
                                break;
                        }


                        // test of login page lms and ok
//                        forms.get(i).selectFirst("#username").val("953611133003");
//                        forms.get(i).selectFirst("#password").val("3920672771");
                    }
                    if (submittingForm(forms.get(i), method, action))
                        break;
                }
                FileWriter fw = new FileWriter(
                        "./data/form/" + id + ".html"
                );
                fw.write(forms.get(i).attributes().html());
                fw.write(forms.get(i).html());
                fw.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Success...");

    }

    public static boolean submittingForm(FormElement form, String method, String action) {
        try {
            String formUrl = StaticAttributes.baseUrl + action.substring(1);
            Connection.Method connectMethod = method.toUpperCase().equals("POST") ?
                    Connection.Method.POST : Connection.Method.GET;

            Connection.Response goToFormPage = Jsoup.connect(formUrl)
                    .userAgent(StaticAttributes.USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();

            Connection.Response connection = Jsoup
                    .connect(formUrl)
                    .userAgent(StaticAttributes.USER_AGENT)
                    .cookies(goToFormPage.cookies())
                    .data(form.formData())
                    .method(connectMethod)
                    .execute();

            System.out.println(connection.url());

            return !connection.url().toString().equals(formUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        boolean b = !FILTERS.matcher(href).matches() && !data.contains(href);
        if (!external) {
            return href.startsWith(baseUrl) && b;
        }
        return b;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().toLowerCase();
        System.out.println("Crawled: " + url);
        data.add(url);
        if (page.getParseData() instanceof HtmlParseData) {
            StaticAttributes.saveHtml(((HtmlParseData) page.getParseData()).getHtml(), url);
        }

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();
            form(html);
        }
    }

    public static void main(String[] arg){
        form("<body class=\"layout_active_theme_azad\" id=\"global_page_core-index-index\" style=\"zoom: 1;\">\n" +
                "  <script type=\"javascript/text\">\n" +
                "    if(DetectIpad()){\n" +
                "      $$('a.album_main_upload').setStyle('display', 'none');\n" +
                "      $$('a.album_quick_upload').setStyle('display', 'none');\n" +
                "      $$('a.icon_photos_new').setStyle('display', 'none');\n" +
                "    }\n" +
                "  </script>  \n" +
                "  \n" +
                "  <div id=\"global_header\">\n" +
                "    <div class=\"layout_page_header\">\n" +
                "<div class=\"generic_layout_container layout_main\">\n" +
                "<div class=\"generic_layout_container layout_core_html_block\">\n" +
                "<script type=\"text/javascript\">\n" +
                " \n" +
                "  var pk = jQuery.noConflict(); \n" +
                "</script>\n" +
                "<script>\n" +
                "pk(document).ready(function(){\n" +
                " \t\n" +
                "  var bb=pk(\"div.layout_main > div.layout_right\").html();\n" +
                "  var dd=pk.trim(bb);\n" +
                "  if(dd==\"\")\n" +
                "  {\n" +
                "\tpk(\"div.layout_main > div.layout_right\").css(\"display\",\"none\")\n" +
                "  }\n" +
                "});\n" +
                "</script>\n" +
                "<script>\n" +
                "\n" +
                "pk(document).ready(function(){\n" +
                "\t\n" +
                "\tfunction checkPersian( firstChar ) {\n" +
                "    if( typeof this.characters == 'undefined' )\n" +
                "        this.characters = ['ا','ب','پ','ت','س','ج','چ','ح','خ','د','ذ','ر','ز','ژ','س','ش','ص','ض','ط','ظ','ع','غ','ف','ق','ک','گ','ل','م','ن','و','ه','ی'];\n" +
                "    return this.characters.indexOf( firstChar ) != -1;\n" +
                "}\n" +
                "\n" +
                "function checkInput(){\n" +
                "    jQuery('div.compose-content').css( 'direction', checkPersian( jQuery('div.compose-content').val().substr( 0, 1 ) ) ? 'rtl' : 'ltr' );\n" +
                "\t\n" +
                "\tvar dirvar = pk('div.compose-content').css('direction');\n" +
                "\tif(dirvar=='rtl')\n" +
                "\t{\n" +
                "\t\t\n" +
                "\t\tpk('div.compose-content').html('<div style=\"direction:rtl\">' + pk('div.compose-content').html() + '</div>');\n" +
                "\t}\n" +
                "\telse\n" +
                "\t{\n" +
                "\t\tpk('div.compose-content').html('<div style=\"direction:ltr\">' + pk('div.compose-content').html() + '</div>');\n" +
                "\t}\n" +
                "\t//oldvalue = pk('div.compose-content').html();\n" +
                "\t\n" +
                "}\n" +
                "//pk('div.compose-content').change( checkInput );\n" +
                "//pk('div.compose-content').keydown( checkInput );\n" +
                "//pk('div.compose-content').keyup( checkInput );\n" +
                "\n" +
                "\n" +
                "\t\n" +
                "\t});\n" +
                "\n" +
                "</script>\n" +
                "\n" +
                "\n" +
                " <script type=\"text/javascript\">\n" +
                " \n" +
                "  var pk = jQuery.noConflict(); \n" +
                "</script>\n" +
                "<script>\n" +
                "pk(document).ready(function(){\n" +
                " \t\n" +
                "  var bb=pk(\"div.layout_main > div.layout_right\").html();\n" +
                "  var dd=pk.trim(bb);\n" +
                "  if(dd==\"\")\n" +
                "  {\n" +
                "\tpk(\"div.layout_main > div.layout_right\").css(\"display\",\"none\")\n" +
                "  }\n" +
                "  \n" +
                "  pk(\"body#global_page_core-pages-test1111 div.layout_middle div.layout_advancedarticles_listarticle_block:nth-child(1)\").css('float','right');\n" +
                "  pk(\"body#global_page_core-pages-test1111 div.layout_middle div.layout_advancedarticles_listarticle_block:nth-child(1)\").css('width','50%');\n" +
                "  \n" +
                "  \n" +
                "  pk(\"body#global_page_core-pages-test1111 div.layout_middle div.layout_advancedarticles_listarticle_block:nth-child(2)\").css('float','left');\n" +
                " pk(\"body#global_page_core-pages-test1111 div.layout_middle div.layout_advancedarticles_listarticle_block:nth-child(2)\").css('width','50%');\n" +
                "  pk(\"body#global_page_core-pages-test1111 div.layout_middle div.layout_core_html_block:nth-child(3)\").css('clear','both');\n" +
                "  pk(\"body#global_page_core-pages-test1111 div.layout_middle div.layout_core_html_block:nth-child(3)\").css('paddingTop','30px');\n" +
                "  \n" +
                "  \n" +
                "  pk(\"#global_page_core-index-index .layout_announcement_list_announcements h3\").empty();\n" +
                "  pk(\"#global_page_core-index-index .layout_announcement_list_announcements h3\").append(\"<div>اخبار</div>\");\n" +
                " \n" +
                " pk(\".wall-types table\").css('width','514px');\n" +
                " pk(\".wall-element-external ul\").attr(\"id\",\"pp11\");\n" +
                " \n" +
                "  \n" +
                "});\n" +
                "</script>\n" +
                "</div>\n" +
                "\n" +
                "<div class=\"generic_layout_container layout_core_html_block\">\n" +
                "<div class=\"header_back_pnu\"> \n" +
                "\n" +
                "<div class=\"header_pnu\">\n" +
                "<div class=\"menu_pnu\">\n" +
                "\n" +
                "\n" +
                "<a href=\"/\">صفحه اصلی</a>|&nbsp;\n" +
                "\n" +
                "<a style=\"text-decoration:none;\" href=\"/pages/Helpfile\">راهنمای سامانه</a>|&nbsp;\n" +
                "<a href=\"/help/contact\">تماس با ما</a>|&nbsp;\n" +
                "<a href=\"http://ui.ac.ir/ShowPage.aspx?page_=form&amp;order=show&amp;lang=1&amp;sub=64&amp;PageId=5840&amp;PageIDF=5839&amp;tempname=librarytemp\">کتابخانه مجازی</a> \n" +
                "</div>\n" +
                "\n" +
                "</div>\n" +
                "\n" +
                "\n" +
                "</div>\n" +
                "\n" +
                "</div>\n" +
                "\n" +
                "<div class=\"generic_layout_container layout_core_menu_mini\">\n" +
                "<div id=\"core_menu_mini_menu\">\n" +
                "    <ul>\n" +
                "              <li><a class=\"menu_core_mini core_mini_auth\" href=\"/login/return_url/64-Lw%3D%3D\">ورود</a></li>\n" +
                "          <li><a class=\"menu_core_mini core_mini_signup\" href=\"/signup\">ثبت نام</a></li>\n" +
                "          </ul>\n" +
                "</div>\n" +
                "\n" +
                "<div id=\"display_pnu\" style=\"display:none;\">\n" +
                "</div>\n" +
                "\n" +
                "\n" +
                "<script type=\"text/javascript\">\n" +
                "\n" +
                "\n" +
                "$$('#dispili').set('text',$$('#display_pnu').get('text'));\n" +
                "\n" +
                "\n" +
                "  var notificationUpdater;\n" +
                "\n" +
                "  en4.core.runonce.add(function(){\n" +
                "    if($('global_search_field')){\n" +
                "      new OverText($('global_search_field'), {\n" +
                "        poll: true,\n" +
                "        pollInterval: 500,\n" +
                "        positionOptions: {\n" +
                "          position: ( en4.orientation == 'rtl' ? 'upperRight' : 'upperLeft' ),\n" +
                "          edge: ( en4.orientation == 'rtl' ? 'upperRight' : 'upperLeft' ),\n" +
                "          offset: {\n" +
                "            x: ( en4.orientation == 'rtl' ? -4 : 4 ),\n" +
                "            y: 2\n" +
                "          }\n" +
                "        }\n" +
                "      });\n" +
                "    }\n" +
                "\n" +
                "    if($('notifications_markread_link')){\n" +
                "      $('notifications_markread_link').addEvent('click', function() {\n" +
                "        //$('notifications_markread').setStyle('display', 'none');\n" +
                "        en4.activity.hideNotifications('\\u062a\\u0627\\u0632\\u0647 \\u0647\\u0627 0');\n" +
                "      });\n" +
                "    }\n" +
                "\n" +
                "      });\n" +
                "\n" +
                "\n" +
                "  var toggleUpdatesPulldown = function(event, element, user_id) {\n" +
                "    if( element.className=='updates_pulldown' ) {\n" +
                "      element.className= 'updates_pulldown_active';\n" +
                "      showNotifications();\n" +
                "    } else {\n" +
                "      element.className='updates_pulldown';\n" +
                "    }\n" +
                "  }\n" +
                "\n" +
                "  var showNotifications = function() {\n" +
                "    en4.activity.updateNotifications();\n" +
                "    new Request.HTML({\n" +
                "      'url' : en4.core.baseUrl + 'activity/notifications/pulldown',\n" +
                "      'data' : {\n" +
                "        'format' : 'html',\n" +
                "        'page' : 1\n" +
                "      },\n" +
                "      'onComplete' : function(responseTree, responseElements, responseHTML, responseJavaScript) {\n" +
                "        if( responseHTML ) {\n" +
                "          // hide loading icon\n" +
                "          if($('notifications_loading')) $('notifications_loading').setStyle('display', 'none');\n" +
                "\n" +
                "          $('notifications_menu').innerHTML = responseHTML;\n" +
                "          $('notifications_menu').addEvent('click', function(event){\n" +
                "            event.stop(); //Prevents the browser from following the link.\n" +
                "\n" +
                "            var current_link = event.target;\n" +
                "            var notification_li = $(current_link).getParent('li');\n" +
                "\n" +
                "            // if this is true, then the user clicked on the li element itself\n" +
                "            if( notification_li.id == 'core_menu_mini_menu_update' ) {\n" +
                "              notification_li = current_link;\n" +
                "            }\n" +
                "\n" +
                "            var forward_link;\n" +
                "            if( current_link.get('href') ) {\n" +
                "              forward_link = current_link.get('href');\n" +
                "            } else{\n" +
                "              forward_link = $(current_link).getElements('a:last-child').get('href');\n" +
                "            }\n" +
                "\n" +
                "            if( notification_li.get('class') == 'notifications_unread' ){\n" +
                "              notification_li.removeClass('notifications_unread');\n" +
                "              en4.core.request.send(new Request.JSON({\n" +
                "                url : en4.core.baseUrl + 'activity/notifications/markread',\n" +
                "                data : {\n" +
                "                  format     : 'json',\n" +
                "                  'actionid' : notification_li.get('value')\n" +
                "                },\n" +
                "                onSuccess : function() {\n" +
                "                  window.location = forward_link;\n" +
                "                }\n" +
                "              }));\n" +
                "            } else {\n" +
                "              window.location = forward_link;\n" +
                "            }\n" +
                "          });\n" +
                "        } else {\n" +
                "          $('notifications_loading').innerHTML = '\\u0627\\u062a\\u0641\\u0627\\u0642 \\u062c\\u062f\\u06cc\\u062f\\u06cc \\u0648\\u062c\\u0648\\u062f \\u0646\\u062f\\u0627\\u0631\\u062f.';\n" +
                "        }\n" +
                "      }\n" +
                "    }).send();\n" +
                "  };\n" +
                "\n" +
                "  /*\n" +
                "  function focusSearch() {\n" +
                "    if(document.getElementById('global_search_field').value == 'Search') {\n" +
                "      document.getElementById('global_search_field').value = '';\n" +
                "      document.getElementById('global_search_field').className = 'text';\n" +
                "    }\n" +
                "  }\n" +
                "  function blurSearch() {\n" +
                "    if(document.getElementById('global_search_field').value == '') {\n" +
                "      document.getElementById('global_search_field').value = 'Search';\n" +
                "      document.getElementById('global_search_field').className = 'text suggested';\n" +
                "    }\n" +
                "  }\n" +
                "  */\n" +
                "</script></div>\n" +
                "</div>\n" +
                "</div>\n" +
                "  </div>\n" +
                "  <div id=\"global_wrapper\">\n" +
                "    <div id=\"global_content\">\n" +
                "            <div class=\"layout_page_core_index_index\">\n" +
                "<div class=\"generic_layout_container layout_main\">\n" +
                "<div class=\"generic_layout_container layout_right\">\n" +
                "<div class=\"generic_layout_container layout_user_login_or_signup\">\n" +
                "\n" +
                "  <h3>\n" +
                "     ورود یا <a class=\"wp_init\" href=\"/signup\">عضویت</a>   </h3>\n" +
                "\n" +
                "  <form class=\"global_form_box\" id=\"user_form_login\" action=\"/login\" enctype=\"application/x-www-form-urlencoded\" method=\"post\"><div><div><div class=\"form-elements\">\n" +
                "<div class=\"form-wrapper\" id=\"username-wrapper\"><div class=\"form-label\" id=\"username-label\"><label class=\"required\" for=\"username\">نام کاربری</label></div>\n" +
                "<div class=\"form-element\" id=\"username-element\">\n" +
                "<input name=\"username\" tabindex=\"1\" class=\"text\" id=\"username\" autofocus=\"autofocus\" type=\"text\" value=\"\"></div></div>\n" +
                "<div class=\"form-wrapper\" id=\"password-wrapper\"><div class=\"form-label\" id=\"password-label\"><label class=\"required\" for=\"password\">رمز عبور</label></div>\n" +
                "<div class=\"form-element\" id=\"password-element\">\n" +
                "<input name=\"password\" tabindex=\"2\" id=\"password\" type=\"password\" value=\"\"></div></div>\n" +
                "<div class=\"form-wrapper\" id=\"buttons-wrapper\"><fieldset id=\"fieldset-buttons\">\n" +
                "<div class=\"form-wrapper\" id=\"submit-wrapper\"><div class=\"form-label\" id=\"submit-label\">&nbsp;</div><div class=\"form-element\" id=\"submit-element\">\n" +
                "<button name=\"submit\" tabindex=\"3\" id=\"submit\" type=\"submit\">ورود</button></div></div>\n" +
                "<div class=\"form-wrapper\" id=\"remember-wrapper\"><div class=\"form-label\" id=\"remember-label\">&nbsp;</div>\n" +
                "<div class=\"form-element\" id=\"remember-element\">\n" +
                "<input name=\"remember\" type=\"hidden\" value=\"\"><input name=\"remember\" tabindex=\"4\" id=\"remember\" type=\"checkbox\" value=\"1\">\n" +
                "<label class=\"optional\" for=\"remember\">مرا به خاطر بسپار</label></div></div></fieldset></div>\n" +
                "\n" +
                "<input name=\"return_url\" id=\"return_url\" type=\"hidden\" value=\"\"></div></div></div></form>\n" +
                "  \n" +
                "<br><span><a class=\"wp_init\" href=\"/user/auth/forgot\"> رمزتان را فراموش کرده اید؟ </a></span></div>\n" +
                "</div>\n" +
                "\n" +
                "<div class=\"generic_layout_container layout_middle\">\n" +
                "<div class=\"generic_layout_container layout_announcement_list_announcements\"><h3><div>اخبار</div></h3>\n" +
                "\n" +
                "<ul class=\"announcements\">\n" +
                "      <li>\n" +
                "      <div class=\"announcements_title\">\n" +
                "        ارزشیابی رایانه ایی تدریس اعضای هیات علمی      </div>\n" +
                "      <div class=\"announcements_info\">\n" +
                "        <span class=\"announcements_author\">\n" +
                "           ارسال شده بوسیله <a class=\"wp_init\" href=\"/profile/lmsadmin\"> واحد آموزش الکترونیکی-یاوری</a> <span title=\"۲۶ آذر ۹۸, ۰۹:۳۹ صبح\" class=\"timestamp\">۲۶ آذر ۹۸, ۰۹:۳۹ صبح</span>         </span>\n" +
                "      </div>\n" +
                "      <div class=\"announcements_body\">\n" +
                "        <p><a class=\"wp_init\" href=\"http://lms.ui.ac.ir/public/linkfile/1576391516.pdf\"><img width=\"455\" height=\"281\" class=\"null\" alt=\"\" src=\"http://lms.ui.ac.ir/public/linkfile/1576391155.jpg\"></a></p>      </div>\n" +
                "    </li>\n" +
                "      <li>\n" +
                "      <div class=\"announcements_title\">\n" +
                "        مشکل در ورود به پرتال      </div>\n" +
                "      <div class=\"announcements_info\">\n" +
                "        <span class=\"announcements_author\">\n" +
                "           ارسال شده بوسیله <a class=\"wp_init\" href=\"/profile/lmsadmin\"> واحد آموزش الکترونیکی-یاوری</a> <span title=\"۷ اسفند ۹۷, ۰۱:۱۰ عصر\" class=\"timestamp\">۷ اسفند ۹۷, ۰۱:۱۰ عصر</span>         </span>\n" +
                "      </div>\n" +
                "      <div class=\"announcements_body\">\n" +
                "        <p><span style=\"color: #000080;\">اساتید گرامی دانشگاه اصفهان در صورتیکه برای ورود به پرتال یادگیری الکترونیکی دچار مشکل گذر واژه می باشید؛ از طریق سامانه اتوماسیون، پیامی برای سرکار خانم یاوری کارشناس و پشتیبان پرتال یادگیری الکترونیکی ارسال نمائید و یا با شماره 37932359 تماس بگیرید.</span></p>      </div>\n" +
                "    </li>\n" +
                "      <li>\n" +
                "      <div class=\"announcements_title\">\n" +
                "        راهنماي ارائه طرح درس      </div>\n" +
                "      <div class=\"announcements_info\">\n" +
                "        <span class=\"announcements_author\">\n" +
                "           ارسال شده بوسیله <a class=\"wp_init\" href=\"/profile/lmsadmin\"> واحد آموزش الکترونیکی-یاوری</a> <span title=\"۱۴ شهريور ۹۷, ۰۱:۱۰ عصر\" class=\"timestamp\">۱۴ شهريور ۹۷, ۰۱:۱۰ عصر</span>         </span>\n" +
                "      </div>\n" +
                "      <div class=\"announcements_body\">\n" +
                "        <p style=\"text-align: center;\">&nbsp;</p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\">&nbsp; با توجه به بازنگري و تصويب&nbsp;فرم ارزيابي عملكرد اجرايي (گزارش ترفيع)</span></p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\">&nbsp;اعضاي هيأت علمي در سيصدو سي و دومين جلسه كميته ترفيع مورخ 25 /&nbsp;2 /&nbsp;98 </span></p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\">و لزوم اجراي آن و همچنين&nbsp;استفاده از سامانه گلستان براي اعلام طرح درس،</span></p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"> لازم است اساتيد محترم طرح درس خود را جهت بررسي توسط مدير گروه</span></p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"> در<strong><a class=\"wp_init\" href=\"https://golestan.ui.ac.ir/\"> سامانه گلستان</a></strong> بارگزاري نمايند و در كنار آن جهت اطلاع دانشجويان مي توانند</span></p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"> از پرتال يادگيري الكترونيكي نيز استفاده نمايند.</span></p>\n" +
                "<p style=\"text-align: justify;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"><br></span></p>\n" +
                "<p style=\"text-align: center;\"><strong><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"><a class=\"wp_init\" href=\"http://lms.ui.ac.ir/public/linkfile/1569649135.pdf\">فرم طرح درس</a></span></strong></p>\n" +
                "<p style=\"text-align: center;\"><strong><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"><br></span></strong></p>\n" +
                "<p style=\"text-align: center;\"><strong><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"><a class=\"wp_init\" href=\"http://lms.ui.ac.ir/public/linkfile/1568448374.pdf\">راهنماي ارائه طرح درس در سامانه گلستان</a></span></strong></p>\n" +
                "<p style=\"text-align: center;\"><span style=\"font-size: medium; font-family: arial, helvetica, sans-serif;\"><br></span></p>\n" +
                "<p style=\"text-align: center;\"><a class=\"wp_init\" href=\"http://lms.ui.ac.ir/public/linkfile/1536136804.pdf\"><span style=\"font-size: medium;\"><strong>راهنماي ارائه طرح درس در پرتال يادگيري الكترونيكي</strong></span></a></p>      </div>\n" +
                "    </li>\n" +
                "  </ul>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "\n" +
                "<div class=\"generic_layout_container layout_bottom\">\n" +
                "\n" +
                "</div>\n" +
                "</div>\n" +
                "          </div>\n" +
                "  </div>\n" +
                "  <div id=\"global_footer\">\n" +
                "    <div class=\"layout_page_footer\">\n" +
                "<div class=\"generic_layout_container layout_main\">\n" +
                "<div class=\"generic_layout_container layout_core_html_block\">\n" +
                "<div style=\"direction:rtl;text-align:right;padding:20px;\">\n" +
                "<span style=\"font-size:10px;\">\n" +
                "</span>\n" +
                "<span style=\"font-size:10px;\">\n" +
                "تمامی حقوق استفاده از این سامانه متعلق به دانشگاه اصفهان می باشد</span>\n" +
                "<a href=\"http://masir.net\">\n" +
                "<span style=\"float:left;font-size:10px;text-decoration:none;\">\n" +
                "طراحی و توسعه: شرکت مسیر فن آوری اطلاعات\n" +
                "</span>\n" +
                "</a>\n" +
                "\n" +
                "\n" +
                "</div>\n" +
                "\n" +
                "</div></div>\n" +
                "\n" +
                "<div class=\"generic_layout_container layout_core_html_block\">\n" +
                "<script>\n" +
                "if($$('#language').get('value') == \"en\"){\n" +
                "$$('.wall-types table').setStyle('float','right');\n" +
                "$$('#core_menu_mini_menu').setStyle('float','right').setStyle('height','38px').setStyle('left','-229px').setStyle('z-index','999999999');\n" +
                "$$('.wall-types').setStyle('top','254');\n" +
                "$$('.menu_pnu').setStyle('z-index','999999999');\n" +
                "}\n" +
                "</script>\n" +
                "\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "  \n" +
                "  <div id=\"janrainEngageShare\" style=\"display:none\">Share</div>\n" +
                "\n" +
                "</body>");
    }
}