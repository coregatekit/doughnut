<template>
  <div class="note-image daisy:text-center" v-if="!!noteImage">
    <div style="position: relative; display: inline-block" id="note-image">
      <img :src="noteImage" />
      <svg
        v-if="!!imageMask"
        viewBox="0 0 100 100"
        style="
          position: absolute;
          top: 0;
          left: 0;
          color: #11f1f1;
          width: 100%;
          height: 100%;
        "
      >
        <template v-for="item in getMasks()" :key="item.index">
          <rect
            :x="item.x"
            :y="item.y"
            :width="item.width"
            :height="item.height"
            :style="`fill:blue;stroke:pink;stroke-width:1;fill-opacity:${opacity};stroke-opacity:0.8`"
          />
        </template>
      </svg>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  noteImage: String,
  imageMask: String,
  opacity: Number,
})

const createGroups = (arr, perGroup) => {
  const numGroups = Math.ceil(arr.length / perGroup)
  return new Array(numGroups)
    .fill("")
    .map((_, i) => arr.slice(i * perGroup, (i + 1) * perGroup))
}

const getMasks = () => {
  return createGroups(props.imageMask.split(/\s+/), 4).map((arr, index) => {
    const [x, y, width, height] = arr
    return { index, x, y, width, height }
  })
}
</script>

<style lang="sass" scoped>
.note-image
  width: 100%
  height: 100%
  img
    max-width: 100%
    max-height: 100%
</style>
