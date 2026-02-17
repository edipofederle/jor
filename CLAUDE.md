# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Start dev server with hot-reload (serves on http://localhost:8080)
npx shadow-cljs watch app

# Production build
npx shadow-cljs release app

# Open a browser REPL (after `watch` is running)
npx shadow-cljs cljs-repl app
```

There are no tests in this project.

## Architecture

**jor** is a ClojureScript single-page app for visualising woodworking joints in 3D using Three.js. Stack: shadow-cljs + Reagent 1.2 + re-frame 1.4 + Three.js 0.165.

### Data flow

```
re-frame db  →  subscriptions  →  add-watch (in canvas component)  →  three/rebuild-joint!
```

The Three.js scene is **not** managed by re-frame. It lives in a private `defonce state` atom in `scene/three.cljs`. The canvas Reagent component (`views/app.cljs`) bridges the two worlds: it calls `rf/subscribe` for `:active-joint`, `:merged-joint-params`, and `:explode-factor`, then uses `add-watch` on those reactions (instead of normal Reagent reactivity) so that REPL dispatches also trigger scene rebuilds. All three watches are debounced through a single `js/setTimeout 0` to coalesce rapid multi-sub updates from one dispatch into a single `rebuild-joint!` call.

### Joint definition schema

Every joint is a plain map with:

```clojure
{:id          keyword
 :label       string
 :doc         string
 :params      {keyword number ...}   ; default params
 :min-explode float                  ; minimum explode factor to avoid Z-fighting
 :parts       [{:id kw :label str :explode-dir [x y z]} ...]
 :build-fn    (fn [params] {part-id THREE.Group ...})
 :cut-seq     [{:step n :label str :part kw-or-nil} ...]}
```

`joints/registry.cljs` holds the master map and ordered list. To add a joint, create a new namespace under `src/jor/joints/`, define a `definition` var following the schema, and require + register it in the registry.

### Coordinate conventions

Each joint picks its own axis orientation; the conventions are documented in comments at the top of each joint file. General rule: parts that interlock must have their mating geometry centred at the world origin so the explode system can cleanly separate them.

### Explode system

`geometry/explode.cljs` computes per-part XYZ offsets from the joint's `:parts` `:explode-dir` vectors scaled by `explode-factor * 80 mm`. `three/rebuild-joint!` clamps the factor to `(max user-factor (:min-explode joint-def))` so interlocking geometry never interpenetrates and Z-fights at rest. When adding a joint, set `:min-explode` to `protrusion_mm / (2 * 80)` (the minimum needed to separate interlocking faces).

### Three.js renderer

`scene/three.cljs` owns the renderer (`logarithmicDepthBuffer: true` for depth precision), camera (`PerspectiveCamera 45°, far=1000`), OrbitControls, and render loop (`requestAnimationFrame`). `rebuild-joint!` disposes old mesh geometries before rebuilding to prevent GPU memory leaks. Three.js objects must never be put into re-frame's app-db (not serialisable).

### Params / overrides

`db.cljs` stores per-joint overrides as `{joint-id {param-key value}}` under `:joint-params`. The `:merged-joint-params` subscription merges these over each joint's `:params` defaults. Events in `events/joints.cljs` update this map; the Three.js scene rebuilds automatically via the watches.
