# AriAI Privacy Policy

**Effective date: 2 November 2026**

This policy describes the AriAI Android app ("AriAI"). It applies to the app distributed from Google Play and to data handled by the app. The policy is published at [github.com/redxbox/ariai](https://github.com/redxbox/ariai).

## The short version

AriAI has no AriAI account, developer-operated chat backend, advertising SDK, or analytics/crash-reporting service. Your conversations and files are stored in the app's private storage on your device. When you choose to use an AI provider, search service, speech-recognition service, URL/repository reader, or MCP server, the information needed for that feature is sent to that service. Those services are operated by you or by third parties, and their own privacy terms apply.

## Information handled by AriAI

Depending on the features you use, AriAI handles:

- Chat titles, prompts, conversation history, model responses, agent instructions, and tool results.
- Files you select or create in the app, including images, documents, audio/video, generated media, and workspace files.
- Provider and endpoint settings, model preferences, search settings, and MCP server configuration.
- API keys and MCP authentication values that you enter.
- Speech-recognition text returned by the Android speech-recognition service.

Chats, settings, and files are kept in app-private storage on your device. AriAI does not separately encrypt chat history or file contents. Provider, search, and MCP credentials are encrypted with an AES key held by Android Keystore before they are saved. Android cloud backup is disabled for the app. Local data remains until you delete it in AriAI, clear the app's storage, or uninstall the app; uninstalling or clearing storage can also remove credentials needed to reconnect your services.

## When information is sent off your device

AriAI does not send your data to an AriAI-operated server. A feature sends data to a service only when you configure or invoke it:

- **AI providers:** When you send a prompt, the selected provider/endpoint receives the conversation context and any selected attachments needed to answer. If the model uses a tool, relevant tool arguments and results can also be included. The provider may process or retain this information under its own terms and settings. For a custom endpoint, the endpoint operator is responsible for its handling.
- **Search services:** A web-search query is sent to the search service you select (for example, its configured API). Search results can be passed to the selected AI provider as context. Provider API keys are sent to their configured service for authentication.
- **Voice input and speech output:** AriAI requests microphone access only for voice input. Android's selected speech-recognition service receives audio for transcription and returns text; that service may process audio on-device or remotely under its own policy. AriAI does not intentionally save the raw voice-input recording. Text-to-speech uses the Android speech engine selected on the device.
- **MCP servers:** If you enable MCP for an agent and allow specific tools, AriAI sends the tool arguments to the MCP server you configured and receives its response. The server can process those arguments and may keep its own logs. Authentication values are sent in request headers, not in the URL. Remote MCP connections require HTTPS; loopback development endpoints may use local HTTP.
- **URL and public-repository tools:** When you ask AriAI to read a URL, it requests that URL. The optional GitHub repository reader makes read-only requests to GitHub's public API for a repository URL you provide. Retrieved content may then be included in a prompt sent to your selected AI provider.

Internet requests use HTTPS where supported and required by the endpoint. Plain HTTP is blocked except for device-local development endpoints. If you configure a third-party or self-hosted endpoint, review its security and privacy practices before sending information.

## Permissions and device access

The app requests internet access to contact the services you configure and microphone access when you start voice input. Media and document selection uses Android's system pickers, so the app can access only the items you choose. AriAI does not request contacts, location, or advertising-ID permissions.

## Sharing, advertising, and diagnostics

AriAI contains no ads and does not include an analytics or crash-reporting SDK. The app does not sell personal information. Data is shared with the external service you choose only as needed to provide the feature you invoked; those services act under their own terms, not this policy. Android and the service providers may have their own system-level logging or diagnostic practices.

## Your choices and deletion

You can choose which provider, search service, MCP server, and agent tools to use; avoid sending sensitive information; remove saved credentials and configurations in the app; delete chats and generated files; revoke microphone permission in Android settings; or stop using a third-party service. To remove all app-local data, clear AriAI's storage or uninstall it. Deletion in AriAI cannot remove information already received or retained by a provider, speech service, search service, GitHub, or MCP server; contact that service directly for its deletion controls.

## Children

AriAI is a general-purpose tool and is not designed to collect information from children. Parents or guardians should supervise children's use of AI services and MCP tools.

## Changes and contact

This policy may be updated when app behavior changes. The current version is published at the repository link above. For privacy questions or requests, use the [AriAI GitHub issue tracker](https://github.com/redxbox/ariai/issues). GitHub issues are public: do not include prompts, API keys, authentication values, personal information, or other confidential data in an issue.

---

# سیاست حریم خصوصی AriAI

**تاریخ اجرا: ۲ نوامبر ۲۰۲۶**

این سیاست دربارهٔ برنامهٔ اندرویدی AriAI است و نحوهٔ پردازش داده‌ها توسط برنامه را توضیح می‌دهد. نسخهٔ جاری در [مخزن عمومی AriAI در GitHub](https://github.com/redxbox/ariai) منتشر می‌شود.

## خلاصه

AriAI حساب کاربری، سرور گفتگو که توسط توسعه‌دهنده اداره شود، تبلیغات، ابزار تحلیل رفتار یا سرویس گزارش خرابی ندارد. گفتگوها و فایل‌ها در فضای خصوصی برنامه روی دستگاه شما نگهداری می‌شوند. وقتی از ارائه‌دهندهٔ هوش مصنوعی، جستجو، تشخیص گفتار، خواندن نشانی/مخزن یا سرور MCP استفاده می‌کنید، اطلاعات لازم برای آن قابلیت به همان سرویس ارسال می‌شود. آن سرویس‌ها را شما یا اشخاص ثالث اداره می‌کنند و سیاست‌های خودشان بر پردازش دادهٔ دریافتی حاکم است.

## اطلاعاتی که AriAI پردازش می‌کند

بسته به قابلیت‌های استفاده‌شده، برنامه می‌تواند عنوان و محتوای گفتگو، پاسخ مدل، دستورهای Agent، نتیجهٔ ابزار، فایل‌های انتخابی یا ساخته‌شده، تنظیمات ارائه‌دهنده و MCP، نشانی endpoint، کلیدهای API و اطلاعات احراز هویت را پردازش کند. متن تشخیص‌داده‌شدهٔ گفتار نیز از سرویس تشخیص گفتار اندروید دریافت می‌شود.

گفتگوها، تنظیمات و فایل‌ها در فضای خصوصی برنامه روی دستگاه ذخیره می‌شوند. محتوای گفتگو و فایل‌ها به‌صورت جداگانه توسط خود AriAI رمزگذاری نمی‌شود. اعتبارنامه‌های ارائه‌دهنده، جستجو و MCP پیش از ذخیره با AES و کلیدی که در Android Keystore نگهداری می‌شود رمزگذاری می‌شوند. پشتیبان‌گیری ابری اندروید برای برنامه غیرفعال است. دادهٔ محلی تا زمانی باقی می‌ماند که آن را در AriAI حذف کنید، فضای ذخیره‌سازی برنامه را پاک کنید یا برنامه را حذف کنید؛ پاک‌کردن داده یا حذف برنامه ممکن است اعتبارنامه‌های لازم برای اتصال دوباره را نیز از بین ببرد.

## ارسال داده به خارج از دستگاه

AriAI داده‌ها را به سروری که توسعه‌دهنده اداره کند نمی‌فرستد. داده فقط هنگام پیکربندی یا استفاده از قابلیت مربوطه به سرویس دیگری ارسال می‌شود:

- **ارائه‌دهندهٔ هوش مصنوعی:** با ارسال پیام، زمینهٔ لازم گفتگو و پیوست‌های انتخابی برای پاسخ به endpoint انتخاب‌شده فرستاده می‌شود. اگر مدل از ابزار استفاده کند، آرگومان‌ها و نتیجهٔ ابزار نیز ممکن است در زمینهٔ درخواست قرار گیرند. ارائه‌دهنده ممکن است طبق شرایط و تنظیمات خودش داده را پردازش یا نگهداری کند. در endpoint سفارشی، مسئول پردازش، گردانندهٔ همان endpoint است.
- **سرویس جستجو:** عبارت جستجو به سرویس انتخاب‌شده فرستاده می‌شود و نتیجه ممکن است به‌عنوان زمینه در اختیار ارائه‌دهندهٔ هوش مصنوعی قرار گیرد. کلید API برای احراز هویت به سرویس مربوطه فرستاده می‌شود.
- **ورودی و خروجی صوتی:** AriAI فقط هنگام شروع ورودی صوتی از میکروفون استفاده می‌کند. سرویس تشخیص گفتار انتخاب‌شده در اندروید صدا را برای تبدیل به متن دریافت می‌کند و ممکن است آن را محلی یا از راه دور پردازش کند. AriAI عمداً فایل خام صدای ورودی را ذخیره نمی‌کند. تبدیل متن به گفتار از موتور انتخاب‌شدهٔ اندروید استفاده می‌کند.
- **سرورهای MCP:** اگر MCP را برای یک Agent فعال کنید و ابزارهای مشخصی را مجاز کنید، آرگومان ابزار به سرور MCP پیکربندی‌شده فرستاده و پاسخ آن دریافت می‌شود. گردانندهٔ سرور ممکن است آرگومان‌ها را پردازش یا در گزارش‌های خود نگهداری کند. اطلاعات احراز هویت در هدر درخواست فرستاده می‌شود، نه در نشانی URL. اتصال MCP از راه دور نیازمند HTTPS است؛ endpoint محلی توسعه ممکن است از HTTP محلی استفاده کند.
- **خواندن نشانی و مخزن عمومی:** هنگام درخواست خواندن یک نشانی، برنامه همان نشانی را درخواست می‌کند. ابزار اختیاری GitHub فقط برای مخزن عمومی‌ای که شما مشخص می‌کنید، درخواست خواندنی به API عمومی GitHub می‌فرستد. محتوای دریافت‌شده ممکن است سپس در درخواست ارسالی به مدل انتخابی قرار گیرد.

درخواست‌های اینترنتی در صورت پشتیبانی و الزام endpoint از HTTPS استفاده می‌کنند. HTTP ساده مسدود است، جز برای endpoint توسعهٔ محلی روی همین دستگاه. پیش از ارسال اطلاعات به endpoint شخص ثالث یا میزبانی‌شده توسط خودتان، امنیت و سیاست حریم خصوصی آن را بررسی کنید.

## مجوزها و دسترسی دستگاه

برنامه برای ارتباط با سرویس‌هایی که پیکربندی می‌کنید به اینترنت و هنگام شروع ورودی صوتی به میکروفون نیاز دارد. انتخاب عکس، ویدیو و سند از انتخاب‌گر سیستمی اندروید انجام می‌شود و برنامه فقط به مواردی دسترسی دارد که خودتان انتخاب می‌کنید. AriAI مجوز مخاطبان، مکان یا شناسهٔ تبلیغاتی درخواست نمی‌کند.

## اشتراک‌گذاری، تبلیغات و عیب‌یابی

AriAI تبلیغ، SDK تحلیل رفتار یا SDK گزارش خرابی ندارد و اطلاعات شخصی را نمی‌فروشد. داده فقط در حد ارائهٔ قابلیتی که انتخاب کرده‌اید با سرویس مربوطه به اشتراک گذاشته می‌شود؛ آن سرویس‌ها تابع شرایط خود هستند. اندروید و ارائه‌دهندگان سرویس ممکن است رویه‌های ثبت گزارش و عیب‌یابی سیستمی خود را داشته باشند.

## انتخاب‌های شما و حذف داده

شما می‌توانید ارائه‌دهنده، سرویس جستجو، سرور MCP و ابزارهای Agent را انتخاب کنید؛ از ارسال اطلاعات حساس خودداری کنید؛ اعتبارنامه‌ها و پیکربندی‌ها را از برنامه حذف کنید؛ گفتگوها و فایل‌های ساخته‌شده را پاک کنید؛ مجوز میکروفون را در تنظیمات اندروید لغو کنید؛ یا استفاده از یک سرویس را متوقف کنید. برای حذف همهٔ داده‌های محلی، فضای ذخیره‌سازی AriAI را پاک یا برنامه را حذف کنید. حذف از AriAI اطلاعاتی را که قبلاً به ارائه‌دهنده، سرویس گفتار، جستجو، GitHub یا سرور MCP رسیده است پاک نمی‌کند؛ برای حذف آن‌ها باید از ابزارهای همان سرویس استفاده کنید.

## کودکان

AriAI ابزاری عمومی است و برای گردآوری اطلاعات کودکان طراحی نشده است. والدین و سرپرستان باید استفادهٔ کودکان از سرویس‌های هوش مصنوعی و ابزارهای MCP را نظارت کنند.

## تغییرات و تماس

در صورت تغییر عملکرد برنامه ممکن است این سیاست به‌روزرسانی شود. نسخهٔ جاری در پیوند مخزن بالا منتشر می‌شود. برای پرسش یا درخواست مرتبط با حریم خصوصی از [بخش Issues مخزن AriAI](https://github.com/redxbox/ariai/issues) استفاده کنید. Issues عمومی هستند؛ پیام گفتگو، کلید API، اطلاعات احراز هویت، اطلاعات شخصی یا محتوای محرمانه را در آن‌ها منتشر نکنید.
