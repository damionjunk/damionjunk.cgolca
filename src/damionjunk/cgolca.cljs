(ns damionjunk.cgolca
  (:require [om.core :as om]
            [om.dom :as dom]
            [cljs.core.async :refer [chan close!]])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]))

(defn gen-board
  "Produces an empty/dead grid of dimensions `x` by `y`.
   0 represents a 'dead' cell.

   Optionally, supply your own `board`.

   2D space is being represented with a 1D sequence of alive/dead states."
  [x y & [board]]
  {:x-max  x
   :y-max  y
   :render :pre
   :board  (or board
               (take (* x y)
                     (repeatedly (fn [& _] (rand-int 2)))
                     ;(repeatedly (constantly 0))
                     ))})

;; Our OM state object to render
(defonce master-board (atom (gen-board 30 25)))

;; http://fn-code.blogspot.com/2015/03/conways-game-of-life-demonstration-and.html
(defn neighbor-xy
  ""
  [[x y]]
  (map vector
       ((juxt dec identity inc dec inc dec identity inc) x)
       ((juxt dec dec dec identity identity inc inc inc) y)))

(defn neighbors
  "Given a board and a x and y position, provide all 2D neighbor positions."
  [{:keys [x-max y-max board]} [px py]]
  ;; Need to get our neighbors in 2D from the 1D space
  (map (fn [[nx ny]] (nth board (+ (* x-max ny) nx)))
       (filter (fn [[nx ny]] (and (>= nx 0) (>= ny 0) (< nx x-max) (< ny y-max))) (neighbor-xy [px py]))))

(defn ->2d
  "Given a `board` and a position in the 1D array, find the 2D position
   on our grid."
  [{:keys [x-max]} x]
  (let [y (int (/ x x-max))]
    [(- x (* x-max y)) y]))

;; Any live cell with fewer than two live neighbours dies, as if caused by under-population.
;; Any live cell with two or three live neighbours lives on to the next generation.
;; Any live cell with more than three live neighbours dies, as if by overcrowding.
;; Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.

(defn next-cell-state
  [board [pos alive]]
  (let [ncount (reduce + (neighbors board (->2d board pos)))]
    (or (and (zero? alive) (= 3 ncount) 1)
        (and (pos? alive) (>= ncount 2) (<= ncount 3) 1)
        0)))

(defn step
  "Given a `board` state, returns an updated (by GOL rules) board state at the
   next time step."
  [board]
  (assoc board :board
               (map-indexed (fn [pos alive]
                              (next-cell-state board [pos alive]))
                            (:board board))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OM : Define a view function to render the CA board state
;;

(defmulti render-board (fn [{:keys [render]}] render))

(defmethod render-board :ascii [{:keys [x-max y-max board]}]
  (apply str
         (interpose "\n"
                    (map (fn [y]
                           (apply str
                                  (map (fn [x]
                                         (nth board (+ (* x-max y) x)))
                                       (range x-max))))
                         (range y-max)))))

(defmethod render-board :pre  [{:keys [x-max y-max board] :as fboard}]
  (dom/pre nil (render-board (assoc fboard :render :ascii))))

(defmethod render-board :html [{:keys [x-max y-max board]}]
  (apply dom/div nil
         (flatten
           (interpose (dom/br nil)
                      (map (partial map (fn [e]
                                          (dom/span #js {:className (if (zero? e) "dg" "gg")} e)))
                           (partition x-max board))))))

(defmethod render-board :default [_])



(defn ca-view [data owner]
      (reify
        om/IRender
        (render [this]
          (render-board data))))

(om/root ca-view master-board
         {:target (. js/document (getElementById "main-area"))})


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; core.async Thread/sleep board update
;;

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn render-loop []
  (go
    (while (:render @master-board)
      (do
        (<! (timeout 750))
        (swap! master-board step)))
    (.log js/console "render loop done.")))

(render-loop)

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; REPL Stuff
;;

(comment

  (dom/pre nil "hi")
  ;; Connect to the nREPL:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  ;; https://github.com/bhauman/lein-figwheel/wiki/Running-figwheel-in-a-Cursive-Clojure-REPL

  ;; Eval these forms and get ready to rumble:
  (ns damionjunk.cgolca)
  (use 'figwheel-sidecar.repl-api)
  (cljs-repl)


  ;; Randomize the board
  (swap! master-board assoc :board (map (fn [_] (rand-int 2)) (:board @master-board)))
  ;; Same thing, since we're not really mapping over the :board data, just the board shape.
  (let [{:keys [x-max y-max]} @master-board]
    (swap! master-board assoc :board (take (* x-max y-max) (repeatedly (fn [& _] (rand-int 2))))))

  ;; Turn off the CPU killer. :)
  (swap! master-board assoc :render nil)
  (swap! master-board assoc :render :html)
  (swap! master-board assoc :render :pre)
  (render-loop)


  (reset! master-board (gen-board 30 30))

  ;; Dump to browser console
  (println (show-board @master-board))

  )


;; Cool way to get neighbors using (juxt)
;; http://fn-code.blogspot.com/2015/03/conways-game-of-life-demonstration-and.html

;; https://en.wikipedia.org/wiki/Conway's_Game_of_Life

;; The universe of the Game of Life is an infinite two-dimensional orthogonal
;; grid of square cells, each of which is in one of two possible states, alive or dead.
;; Every cell interacts with its eight neighbours,
;; which are the cells that are horizontally, vertically,
;; or diagonally adjacent.
;; At each step in time, the following transitions occur:

;; Any live cell with fewer than two live neighbours dies, as if caused by under-population.
;; Any live cell with two or three live neighbours lives on to the next generation.
;; Any live cell with more than three live neighbours dies, as if by overcrowding.
;; Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.