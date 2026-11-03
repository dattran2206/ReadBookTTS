package com.vietreader.tts.data.parser

import java.util.regex.Pattern

/**
 * High-performance English-to-Vietnamese Phonetic Normalizer.
 *
 * Converts common English words, technical IT/Deep Learning terminology,
 * abbreviations, and popular novel names (e.g. Harry Potter) into natural
 * Vietnamese phonetic equivalents for smooth, fluent TTS pronunciation.
 *
 * Features:
 * - Applied exclusively to the audio synthesis pipeline (TTS speech output),
 *   leaving on-screen text 100% authentic and unaltered.
 * - Multi-word phrases are prioritized and matched first.
 * - Uses Unicode lookaround boundaries `(?<!\w)...\b` or `(?<!\w)...(?!\w)`
 *   so that terms ending in symbols (C++, C#, etc.) or preceding punctuation match reliably.
 * - Tricky acronyms that collide with common Vietnamese words (like "AI" vs "ai",
 *   "IT" vs "it", "UI", "OS", "GAN") are strictly case-sensitive.
 * - Micro-second execution time with pre-compiled regex patterns.
 */
object EnglishPhoneticNormalizer {

    private data class PhoneticReplacement(
        val pattern: Pattern,
        val replacement: String
    )

    private val rules: List<PhoneticReplacement>

    init {
        val ruleList = mutableListOf<PhoneticReplacement>()

        fun addRule(english: String, vietnamese: String, caseSensitive: Boolean = false) {
            try {
                val escaped = Pattern.quote(english)
                val flags = if (caseSensitive) {
                    Pattern.UNICODE_CASE
                } else {
                    Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
                }
                val patternStr = "(?<![\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])"
                ruleList.add(
                    PhoneticReplacement(
                        pattern = Pattern.compile(patternStr, flags),
                        replacement = vietnamese
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("EnglishNormalizer", "Failed compiling rule for '$english'", e)
            }
        }

        // =========================================================================
        // 1. DEEP LEARNING & AI / DATA SCIENCE TERMINOLOGY (Multi-word first)
        // =========================================================================
        addRule("deep learning", "đíp lơ ning")
        addRule("machine learning", "mơ sin lơ ning")
        addRule("reinforcement learning", "ri in phót mừn lơ ning")
        addRule("supervised learning", "su pơ vai lơ ning")
        addRule("unsupervised learning", "ăn su pơ vai lơ ning")
        addRule("semi-supervised learning", "xê mi su pơ vai lơ ning")
        addRule("transfer learning", "tran phơ lơ ning")
        addRule("federated learning", "phe đơ rê tít lơ ning")
        addRule("self-supervised learning", "xeo su pơ vai lơ ning")

        addRule("neural network", "niu rờn nét uốc")
        addRule("neural networks", "niu rờn nét uốc")
        addRule("artificial neural network", "a ti phi sồ niu rờn nét uốc")
        addRule("convolutional neural network", "con vô lu sừn niu rờn nét uốc")
        addRule("recurrent neural network", "ri cơ rừn niu rờn nét uốc")
        addRule("generative adversarial network", "gien nơ rê típ át vơ xe ri ờ nét uốc")

        addRule("artificial intelligence", "a ti phi sồ in te li dần")
        addRule("natural language processing", "na tu rờn lang guýt prô xét xing")
        addRule("computer vision", "com piu tơ vi dần")
        addRule("large language model", "lạt lang guýt mô đen")
        addRule("large language models", "lạt lang guýt mô đen")

        addRule("gradient descent", "gờ ra đi ừn đi xen")
        addRule("stochastic gradient descent", "xtô cát tích gờ ra đi ừn đi xen")
        addRule("backpropagation", "bắc pơ pơ gấy sừn")
        addRule("loss function", "lót phăng sừn")
        addRule("cost function", "cót phăng sừn")
        addRule("objective function", "óp giếch tip phăng sừn")
        addRule("activation function", "ác ti vấy sừn phăng sừn")
        addRule("learning rate", "lơ ning rết")
        addRule("batch size", "bát sai")
        addRule("cross entropy", "cờ rót en trô pi")
        addRule("mean squared error", "min xque e rơ")
        addRule("overfitting", "âu vơ phít ting")
        addRule("underfitting", "ăn đơ phít ting")
        addRule("data science", "đa ta sai ừn")
        addRule("big data", "bích đa ta")
        addRule("data set", "đa ta sét")
        addRule("dataset", "đa ta sét")
        addRule("datasets", "đa ta sét")
        addRule("database", "đa ta bết")
        addRule("support vector machine", "xúp pót véc tơ mơ sin")
        addRule("decision tree", "đê si dần tri")
        addRule("random forest", "ran đờm pho rét")
        addRule("linear regression", "li ni ơ ri gờ rét sừn")
        addRule("logistic regression", "lô giít tích ri gờ rét sừn")
        addRule("k-nearest neighbors", "kây nia rét nây bơ")
        addRule("k-means", "kây min")
        addRule("principal component analysis", "prin xi pồ cơm pô nừn ơ na li xít")

        // Modern Generative AI & LLM concepts
        addRule("stable diffusion", "xì tây bồ đi phiu dần")
        addRule("diffusion model", "đi phiu dần mô đen")
        addRule("prompt engineering", "próm en di ni ơ ring")
        addRule("fine-tuning", "phain tun ninh")
        addRule("fine tuning", "phain tun ninh")
        addRule("fine-tune", "phain tun")
        addRule("fine tune", "phain tun")
        addRule("pre-trained", "pri tren")
        addRule("zero-shot", "di rô sót")
        addRule("few-shot", "phiu sót")
        addRule("in-context learning", "in con téc lơ ning")
        addRule("self-attention", "xeo a ten sừn")
        addRule("multi-head attention", "man ti hét a ten sừn")
        addRule("transformer", "tran pho mơ")
        addRule("transformers", "tran pho mơ")
        addRule("embedding", "em bét đinh")
        addRule("embeddings", "em bét đinh")
        addRule("vector database", "véc tơ đa ta bết")
        addRule("temperature", "tem pơ rơ chơ")
        addRule("hallucination", "ha lu si nây sừn")
        addRule("grounding", "grao đinh")

        // Frameworks & Popular Models
        addRule("tensorflow", "ten xo phơ lâu")
        addRule("pytorch", "phai tọt")
        addRule("scikit learn", "xki kít lơn")
        addRule("scikit-learn", "xki kít lơn")
        addRule("hugging face", "hắc ging phết")
        addRule("huggingface", "hắc ging phết")
        addRule("chatgpt", "chát giê pê tê")
        addRule("chat gpt", "chát giê pê tê")
        addRule("gpt-4o", "giê pê tê bốn ô")
        addRule("gpt-4", "giê pê tê bốn")
        addRule("gpt-3.5", "giê pê tê ba chấm năm")
        addRule("openai", "âu pừn ây ai")
        addRule("deepseek", "đíp xích")
        addRule("claude", "cờ lót")
        addRule("gemini", "gê mi ni")
        addRule("midjourney", "mít giơ ni")
        addRule("whisper", "uých pơ")
        addRule("copilot", "cô pai lợt")

        // =========================================================================
        // 2. TECH ACRONYMS
        // Tricky ones that collide with Vietnamese words are strictly CASE-SENSITIVE.
        // =========================================================================
        addRule("AI", "ây ai", caseSensitive = true)
        addRule("A.I.", "ây ai", caseSensitive = true)
        addRule("ML", "em eo", caseSensitive = true)
        addRule("DL", "đi eo", caseSensitive = true)
        addRule("NLP", "en eo pi")
        addRule("CV", "xi vi", caseSensitive = true)
        addRule("LLM", "eo eo em")
        addRule("LLMs", "eo eo em")
        addRule("CNN", "xi en en")
        addRule("RNN", "a en en")
        addRule("ANN", "ây en en")
        addRule("GAN", "ghen", caseSensitive = true)
        addRule("GANs", "ghen", caseSensitive = true)
        addRule("RAG", "rác", caseSensitive = true)
        addRule("LoRA", "lô ra", caseSensitive = true)
        addRule("GPT", "giê pê tê")

        addRule("TTS", "tê tê ét")
        addRule("STT", "ét tê tê")
        addRule("ASR", "ây ét a")
        addRule("OCR", "ô xê a")

        addRule("api", "ây pi ai")
        addRule("apis", "ây pi ai")
        addRule("sdk", "ét đê ca")
        addRule("cpu", "xi pi u")
        addRule("gpu", "gi pi u")
        addRule("gpus", "gi pi u")
        addRule("tpu", "ti pi u")
        addRule("ram", "ram")
        addRule("rom", "rom")
        addRule("ssd", "ét ét đê")
        addRule("hdd", "hát đê đê")
        addRule("usb", "u ét bê")
        addRule("url", "u rờ le")
        addRule("http", "hát tê tê pê")
        addRule("https", "hát tê tê pê ét")
        addRule("html", "hắt tê mờ lờ")
        addRule("css", "xê ét ét")
        addRule("js", "giây ét")
        addRule("json", "giây xơn")
        addRule("xml", "ích mờ lờ")
        addRule("sql", "ét quy eo")
        addRule("nosql", "nô ét quy eo")
        addRule("pdf", "pê đê ép")
        addRule("epub", "ê púp")
        addRule("txt", "tê ích tê")
        addRule("app", "áp")
        addRule("apps", "áp")
        addRule("UI", "u ai", caseSensitive = true)
        addRule("UX", "u ích", caseSensitive = true)
        addRule("OS", "ô ét", caseSensitive = true)
        addRule("iOS", "ai ô ét", caseSensitive = true)
        addRule("ios", "ai ô ét")
        addRule("android", "an roi")
        addRule("iot", "ai ô ti")
        addRule("IT", "ai ti", caseSensitive = true)
        addRule("wifi", "oai phai")
        addRule("wi-fi", "oai phai")
        addRule("bluetooth", "bờ lu tút")

        // Big Tech & Platforms
        addRule("google", "gu gồ")
        addRule("facebook", "phây bắc")
        addRule("meta", "mê ta")
        addRule("microsoft", "mai crô xóp")
        addRule("apple", "áp pồ")
        addRule("amazon", "a ma dôn")
        addRule("nvidia", "en vi đi a")
        addRule("intel", "in teo")
        addRule("github", "gít hắp")
        addRule("gitlab", "gít láp")
        addRule("docker", "đốc cơ")
        addRule("linux", "li nức")
        addRule("ubuntu", "u bun tu")
        addRule("windows", "win đâu")
        addRule("macbook", "mác búc")
        addRule("iphone", "ai phôn")
        addRule("ipad", "ai pát")
        addRule("youtube", "diu túp")
        addRule("tiktok", "tích tốc")
        addRule("telegram", "te le gam")
        addRule("zalo", "da lô")

        // Programming & IT Terms
        addRule("python", "phai thần")
        addRule("java", "gia va")
        addRule("kotlin", "cót lin")
        addRule("javascript", "gia va xờ cờ ríp")
        addRule("typescript", "típ xờ cờ ríp")
        addRule("golang", "gô lang")
        addRule("c++", "xi cộng cộng")
        addRule("c#", "xi thăng")
        addRule("algorithm", "an gô rít")
        addRule("algorithms", "an gô rít")
        addRule("vector", "véc tơ")
        addRule("vectors", "véc tơ")
        addRule("matrix", "ma trích")
        addRule("matrices", "ma trích")
        addRule("tensor", "ten xơ")
        addRule("tensors", "ten xơ")
        addRule("bias", "bai ớt")
        addRule("weight", "uếch")
        addRule("weights", "uếch")
        addRule("epoch", "ê póc")
        addRule("epochs", "ê póc")
        addRule("layer", "lây ơ")
        addRule("layers", "lây ơ")
        addRule("parameter", "pa ra mi tơ")
        addRule("parameters", "pa ra mi tơ")
        addRule("hyperparameter", "hai pơ pa ra mi tơ")
        addRule("hyperparameters", "hai pơ pa ra mi tơ")
        addRule("function", "phăng sừn")
        addRule("functions", "phăng sừn")
        addRule("optimizer", "óp ti mai dơ")
        addRule("optimizers", "óp ti mai dơ")
        addRule("network", "nét uốc")
        addRule("networks", "nét uốc")
        addRule("server", "xơ vơ")
        addRule("servers", "xơ vơ")
        addRule("client", "cờ lai ừn")
        addRule("cloud", "cờ lau")
        addRule("backend", "bắc en")
        addRule("frontend", "phờ rôn en")
        addRule("fullstack", "phun tắc")
        addRule("developer", "đê ve lốp pơ")
        addRule("developers", "đê ve lốp pơ")
        addRule("coder", "cô đơ")
        addRule("hacker", "hắc cơ")
        addRule("programmer", "prô gram mơ")
        addRule("software", "xóp ooe")
        addRule("hardware", "hát ooe")
        addRule("firmware", "phơm ooe")
        addRule("framework", "phờ rem uốc")
        addRule("library", "lai bờ rơ ri")
        addRule("package", "pắc kích")
        addRule("module", "mô đun")
        addRule("debug", "đê bắc")
        addRule("release", "ri lít")
        addRule("build", "bưu")
        addRule("test", "tét")
        addRule("testing", "tét ting")
        addRule("version", "vơ sừn")
        addRule("update", "áp đết")
        addRule("upgrade", "áp gờ rết")
        addRule("download", "đao lót")
        addRule("upload", "áp lót")
        addRule("install", "inxto")
        addRule("setup", "xét ắp")
        addRule("config", "con phích")
        addRule("configuration", "con phi gu rây sừn")
        addRule("online", "on lai")
        addRule("offline", "óp lai")
        addRule("internet", "in tơ nét")
        addRule("website", "uép sai")
        addRule("web", "uép")
        addRule("page", "pếch")
        addRule("link", "linh")
        addRule("click", "clích")
        addRule("file", "phai")
        addRule("folder", "phôn đơ")
        addRule("data", "đa ta")
        addRule("code", "cốt")
        addRule("source code", "xọt cốt")
        addRule("script", "xờ cờ ríp")
        addRule("bug", "bắc")
        addRule("fix", "phích")
        addRule("error", "e rơ")
        addRule("warning", "oan ning")
        addRule("log", "lóc")
        addRule("token", "tô kừn")
        addRule("tokens", "tô kừn")
        addRule("cache", "cát")
        addRule("stream", "xì trim")
        addRule("streaming", "xì tri ming")
        addRule("audio", "ao đi ô")
        addRule("video", "vi đê ô")
        addRule("media", "mê đi a")
        addRule("podcast", "pót cát")
        addRule("smart", "xì mát")
        addRule("smartphone", "xì mát phôn")
        addRule("device", "đê vai")
        addRule("tablet", "táp lét")

        // =========================================================================
        // 3. POPULAR FICTION & NOVEL NAMES (e.g. Harry Potter universe)
        // =========================================================================
        addRule("harry potter", "He-ri Pót-tơ")
        addRule("harry", "He-ri")
        addRule("potter", "Pót-tơ")
        addRule("hermione granger", "Hơ-mai-ơ-ni Gờ-ren-giơ")
        addRule("hermione", "Hơ-mai-ơ-ni")
        addRule("granger", "Gờ-ren-giơ")
        addRule("ron weasley", "Ron Uých-ly")
        addRule("weasley", "Uých-ly")
        addRule("albus dumbledore", "An-bớt Đăm-bồ-đo")
        addRule("dumbledore", "Đăm-bồ-đo")
        addRule("albus", "An-bớt")
        addRule("severus snape", "Xê-vơ-rớt X-nếp")
        addRule("snape", "X-nếp")
        addRule("severus", "Xê-vơ-rớt")
        addRule("lord voldemort", "Loọc Vôn-đơ-mọt")
        addRule("voldemort", "Vôn-đơ-mọt")
        addRule("draco malfoy", "Đơ-ra-cô Man-phoi")
        addRule("malfoy", "Man-phoi")
        addRule("draco", "Đơ-ra-cô")
        addRule("rubeus hagrid", "Ru-bê-ớt Hác-grít")
        addRule("hagrid", "Hác-grít")
        addRule("sirius black", "Xi-ri-ớt Bờ-lách")
        addRule("sirius", "Xi-ri-ớt")
        addRule("remus lupin", "Rê-mớt Lu-pin")
        addRule("lupin", "Lu-pin")
        addRule("minerva mcgonagall", "Mi-nơ-va Mác-gô-na-gan")
        addRule("mcgonagall", "Mác-gô-na-gan")
        addRule("bellatrix lestrange", "Bê-la-trích Lơ-xờ-tranh")
        addRule("lestrange", "Lơ-xờ-tranh")
        addRule("bellatrix", "Bê-la-trích")
        addRule("neville longbottom", "Nê-vin Long-bót-tơm")
        addRule("longbottom", "Long-bót-tơm")
        addRule("neville", "Nê-vin")
        addRule("luna lovegood", "Lu-na Lớp-gút")
        addRule("lovegood", "Lớp-gút")
        addRule("ginny weasley", "Gin-ni Uých-ly")
        addRule("ginny", "Gin-ni")
        addRule("dobby", "Đóp-bi")
        addRule("kreacher", "Cờ-ri-chơ")
        addRule("hogwarts", "Hóc-uốt")
        addRule("gryffindor", "Gờ-ríp-phin-đo")
        addRule("slytherin", "X-li-thơ-rin")
        addRule("ravenclaw", "Rây-vơn-clo")
        addRule("hufflepuff", "Háp-phồ-páp")
        addRule("quidditch", "Quýt-đích")
        addRule("azkaban", "Át-ca-ban")
        addRule("hogsmeade", "Hóc-mít")
        addRule("diagon alley", "Hẻm Xéo")
        addRule("horcrux", "Trường sinh linh giá")
        addRule("horcruxes", "Trường sinh linh giá")
        addRule("deathly hallows", "Bảo bối tử thần")
        addRule("muggle", "Mắc-gồ")
        addRule("muggles", "Mắc-gồ")
        addRule("dementor", "Giám ngục")
        addRule("dementors", "Giám ngục")

        // Common English Titles
        addRule("mr.", "Mít-xtơ")
        addRule("mrs.", "Mít-xít")
        addRule("ms.", "Mít")
        addRule("miss", "Mít")
        addRule("dr.", "Bác sĩ")
        addRule("prof.", "Giáo sư")
        addRule("professor", "Giáo sư")
        addRule("sir", "Sơ")
        addRule("lord", "Loọc")
        addRule("lady", "Lê-đi")

        // =========================================================================
        // 4. EVERYDAY CONVERSATIONAL ENGLISH LOAN WORDS
        // Note: Dangerous homophones with Vietnamese (e.g. "no", "hi") are omitted.
        // =========================================================================
        addRule("okay", "ô kê")
        addRule("ok", "ô kê")
        addRule("hello", "hê lô")
        addRule("bye", "bai")
        addRule("bye bye", "bai bai")
        addRule("goodbye", "gút bai")
        addRule("yes", "dét")
        addRule("thank you", "thenh kiu")
        addRule("thanks", "thenh")
        addRule("please", "pờ lit")
        addRule("sorry", "so ri")
        addRule("welcome", "oen com")
        addRule("game", "gêm")
        addRule("gamer", "gêm mơ")
        addRule("streamer", "xì tri mơ")
        addRule("team", "tim")
        addRule("top", "tóp")
        addRule("hot", "hót")
        addRule("show", "sô")
        addRule("live", "lai")
        addRule("livestream", "lai xì trim")
        addRule("view", "viu")
        addRule("like", "lai")
        addRule("dislike", "đít lai")
        addRule("share", "se")
        addRule("comment", "còm men")
        addRule("post", "bót")
        addRule("status", "xì ta tút")
        addRule("fan", "phan")
        addRule("fans", "phan")
        addRule("idol", "ai đần")
        addRule("super", "su pơ")
        addRule("pro", "prô")
        addRule("max", "mác")
        addRule("plus", "pờ lút")
        addRule("mini", "mi ni")
        addRule("free", "phờ ri")
        addRule("vip", "víp")
        addRule("check", "chếch")
        addRule("checking", "chếch king")
        addRule("checkout", "chếch ao")
        addRule("sale", "xeo")
        addRule("shopping", "sóp ping")
        addRule("store", "xì to")
        addRule("market", "mác két")
        addRule("ship", "síp")
        addRule("shipper", "síp pơ")
        addRule("bill", "biu")
        addRule("card", "cạt")
        addRule("credit", "cờ rê đít")
        addRule("bank", "banh")
        addRule("tour", "tua")
        addRule("travel", "tra vồ")
        addRule("hotel", "hô ten")
        addRule("checkin", "chếch in")
        addRule("check-in", "chếch in")

        rules = ruleList
    }

    /**
     * Normalizes English words and terminology in [text] into smooth Vietnamese phonetics.
     * Guaranteed to be non-blocking and safe for real-time speech pipelines.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return text
        return try {
            var result = text
            for (rule in rules) {
                val matcher = rule.pattern.matcher(result)
                if (matcher.find()) {
                    result = matcher.replaceAll(rule.replacement)
                }
            }
            result
        } catch (e: Exception) {
            android.util.Log.e("EnglishNormalizer", "Error during phonetic normalization", e)
            text
        }
    }
}
