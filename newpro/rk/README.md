# RK-Web 鍓嶇椤圭洰

> 榛戞渤瀛﹂櫌杞欢椤圭洰寮€鍙戠ぞ鍥㈠墠绔」鐩?- 閲嶆瀯鐗?v2.0

## 椤圭洰绠€浠?

RK-Web 鍓嶇椤圭洰鏄粦娌冲闄㈣蒋浠堕」鐩紑鍙戠ぞ鍥㈢殑瀹樻柟缃戠珯锛岄噰鐢ㄧ幇浠ｅ寲鐨勬妧鏈爤閲嶆瀯锛屾彁渚涙柊闂诲姩鎬併€佷綔鍝佸睍绀恒€佹瘮璧涚鐞嗐€佹椿鍔ㄤ俊鎭瓑鍔熻兘銆?

## 鎶€鏈爤

- **妗嗘灦**: Vue 3 (Composition API)
- **鏋勫缓宸ュ叿**: Vite 5.0
- **UI缁勪欢搴?*: Element Plus 2.5
- **鐘舵€佺鐞?*: Pinia 2.1
- **璺敱**: Vue Router 4.2
- **HTTP瀹㈡埛绔?*: Axios 1.6
- **鏍峰紡棰勫鐞?*: SCSS

## 椤圭洰缁撴瀯

```
newpro/rk/
鈹溾攢鈹€ src/
鈹?  鈹溾攢鈹€ api/              # API鎺ュ彛
鈹?  鈹?  鈹溾攢鈹€ news.js
鈹?  鈹?  鈹溾攢鈹€ works.js
鈹?  鈹?  鈹溾攢鈹€ competition.js
鈹?  鈹?  鈹溾攢鈹€ activity.js
鈹?  鈹?  鈹斺攢鈹€ user.js
鈹?  鈹溾攢鈹€ assets/           # 闈欐€佽祫婧?
鈹?  鈹溾攢鈹€ components/       # 鍏叡缁勪欢
鈹?  鈹溾攢鈹€ layouts/          # 甯冨眬缁勪欢
鈹?  鈹?  鈹斺攢鈹€ MainLayout.vue
鈹?  鈹溾攢鈹€ router/           # 璺敱閰嶇疆
鈹?  鈹?  鈹斺攢鈹€ index.js
鈹?  鈹溾攢鈹€ stores/           # 鐘舵€佺鐞?
鈹?  鈹溾攢鈹€ styles/           # 鍏ㄥ眬鏍峰紡
鈹?  鈹?  鈹溾攢鈹€ index.scss
鈹?  鈹?  鈹溾攢鈹€ variables.scss
鈹?  鈹?  鈹溾攢鈹€ mixins.scss
鈹?  鈹?  鈹斺攢鈹€ common.scss
鈹?  鈹溾攢鈹€ utils/            # 宸ュ叿鍑芥暟
鈹?  鈹?  鈹斺攢鈹€ request.js
鈹?  鈹溾攢鈹€ views/            # 椤甸潰缁勪欢
鈹?  鈹?  鈹溾攢鈹€ Home.vue
鈹?  鈹?  鈹溾攢鈹€ News.vue
鈹?  鈹?  鈹溾攢鈹€ Works.vue
鈹?  鈹?  鈹溾攢鈹€ About.vue
鈹?  鈹?  鈹溾攢鈹€ Join.vue
鈹?  鈹?  鈹斺攢鈹€ ...
鈹?  鈹溾攢鈹€ App.vue           # 鏍圭粍浠?
鈹?  鈹斺攢鈹€ main.js           # 鍏ュ彛鏂囦欢
鈹溾攢鈹€ public/               # 鍏叡闈欐€佽祫婧?
鈹溾攢鈹€ index.html            # HTML妯℃澘
鈹溾攢鈹€ vite.config.js        # Vite閰嶇疆
鈹溾攢鈹€ package.json          # 椤圭洰渚濊禆
鈹斺攢鈹€ nginx.conf            # Nginx閰嶇疆

```

## 蹇€熷紑濮?

### 鐜瑕佹眰

- Node.js >= 16.0.0
- npm >= 8.0.0 鎴?pnpm >= 7.0.0

### 瀹夎渚濊禆

```bash
npm install
```

### 寮€鍙戞ā寮?

```bash
npm run dev
```

璁块棶 http://localhost:5173

### 鐢熶骇鏋勫缓

```bash
npm run build
```

鏋勫缓浜х墿鍦?`dist/` 鐩綍

### 棰勮鏋勫缓

```bash
npm run preview
```

## 閮ㄧ讲

### 浣跨敤Nginx閮ㄧ讲

1. 鏋勫缓椤圭洰
```bash
npm run build
```

2. 灏?`dist/` 鐩綍鍐呭澶嶅埗鍒癗ginx鐨勭綉绔欐牴鐩綍

3. 浣跨敤鎻愪緵鐨?`nginx.conf` 閰嶇疆鏂囦欢

4. 閲嶅惎Nginx

### Docker閮ㄧ讲

```dockerfile
FROM node:18-alpine as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## API浠ｇ悊閰嶇疆

寮€鍙戠幆澧傾PI浠ｇ悊閰嶇疆鍦?`vite.config.js`:

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:7659',
      changeOrigin: true
    },
    '/ruoyi': {
      target: 'http://localhost:8099',
      changeOrigin: true
    }
  }
}
```

## 鏍峰紡瑙勮寖

椤圭洰浣跨敤SCSS浣滀负鏍峰紡棰勫鐞嗗櫒锛屽畾涔変簡浠ヤ笅鍙橀噺鍜屾贩鍚堝櫒锛?

- **鍙橀噺** (`variables.scss`): 棰滆壊銆佸瓧浣撱€侀棿璺濄€佸渾瑙掋€侀槾褰辩瓑
- **娣峰悎鍣?* (`mixins.scss`): 鍝嶅簲寮忔柇鐐广€佹枃鏈埅鏂€丗lex甯冨眬绛?
- **閫氱敤绫?* (`common.scss`): 甯哥敤宸ュ叿绫?

## 娴忚鍣ㄦ敮鎸?

- Chrome >= 90
- Firefox >= 88
- Safari >= 14
- Edge >= 90

## 寮€鍙戣鑼?

### 鍛藉悕瑙勮寖

- **缁勪欢鏂囦欢**: PascalCase (濡?`Home.vue`)
- **宸ュ叿鍑芥暟**: camelCase (濡?`formatDate`)
- **甯搁噺**: UPPER_SNAKE_CASE (濡?`API_BASE_URL`)
- **CSS绫诲悕**: kebab-case (濡?`.news-card`)

### 浠ｇ爜椋庢牸

- 浣跨敤 Composition API (`<script setup>`)
- 浣跨敤 TypeScript 绫诲瀷娉ㄨВ锛堝彲閫夛級
- 閬靛惊 ESLint 瑙勫垯
- 浠ｇ爜鏍煎紡鍖栦娇鐢?Prettier

### Git鎻愪氦瑙勮寖

```
feat: 鏂板姛鑳?
fix: 淇bug
docs: 鏂囨。鏇存柊
style: 浠ｇ爜鏍煎紡璋冩暣
refactor: 閲嶆瀯浠ｇ爜
perf: 鎬ц兘浼樺寲
test: 娴嬭瘯鐩稿叧
chore: 鏋勫缓杩囩▼鎴栬緟鍔╁伐鍏风殑鍙樺姩
```

## API鏂囨。

璇︾粏鐨凙PI鎺ュ彛鏂囨。璇锋煡鐪?[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

## 涓昏鍔熻兘妯″潡

### 1. 棣栭〉
- 杞挱鍥惧睍绀?
- 瀛︽牎瑕侀椈
- 閫氱煡鍏憡
- 绀惧洟鐗硅壊
- 绮鹃€変綔鍝?

### 2. 鏂伴椈妯″潡
- 鏂伴椈鍒楄〃
- 鏂伴椈璇︽儏
- 鏂伴椈鎼滅储
- 鍒嗙被娴忚

### 3. 浣滃搧灞曠ず
- 浣滃搧鍒楄〃
- 浣滃搧璇︽儏
- 浣滃搧鐐硅禐
- 娴忚缁熻

### 4. 姣旇禌绠＄悊
- 姣旇禌鍒楄〃
- 姣旇禌璇︽儏
- 姣旇禌鎶ュ悕
- 浣滃搧鎻愪氦

### 5. 鍏朵粬椤甸潰
- 绀惧洟姒傚喌
- 鍔犲叆鎴戜滑
- 鏍″弸椋庨噰
- 鑱旂郴鎴戜滑

## 鎬ц兘浼樺寲

- 浠ｇ爜鍒嗗壊鍜屾噿鍔犺浇
- 鍥剧墖鎳掑姞杞?
- Gzip鍘嬬缉
- 闈欐€佽祫婧愮紦瀛?
- CDN鍔犻€燂紙鍙€夛級

## 甯歌闂

### Q: 濡備綍淇敼涓婚棰滆壊锛?

A: 淇敼 `src/styles/variables.scss` 涓殑棰滆壊鍙橀噺銆?

### Q: 濡備綍娣诲姞鏂扮殑椤甸潰锛?

A: 鍦?`src/views/` 涓嬪垱寤烘柊鐨刅ue缁勪欢锛屽苟鍦?`src/router/index.js` 涓坊鍔犺矾鐢遍厤缃€?

### Q: 濡備綍閰嶇疆API鍦板潃锛?

A: 淇敼 `vite.config.js` 涓殑 proxy 閰嶇疆锛屾垨淇敼 `src/utils/request.js` 涓殑 baseURL銆?

## 璐＄尞鎸囧崡

1. Fork 鏈粨搴?
2. 鍒涘缓鐗规€у垎鏀?(`git checkout -b feature/AmazingFeature`)
3. 鎻愪氦鏇存敼 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 鎺ㄩ€佸埌鍒嗘敮 (`git push origin feature/AmazingFeature`)
5. 寮€鍚?Pull Request

## 璁稿彲璇?

MIT License

## 鑱旂郴鏂瑰紡

- 椤圭洰鍦板潃: https://github.com/example/rk-web
- 閭: contact@rk-web.org
- 鍦板潃: 榛戞渤瀛﹂櫌

---

**娉ㄦ剰**: 鏈」鐩负閲嶆瀯鐗堟湰锛屽缓璁湪瀹屾垚API鎺ュ彛寮€鍙戝悗杩涜鍏ㄩ潰娴嬭瘯銆