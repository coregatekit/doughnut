<template>
  <div
    class="note-recent-update-indicator daisy:flex daisy:flex-col daisy:gap-1 daisy:py-2"
    :style="`color: ${bgColor}`"
  >
    <slot />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue"

export default defineComponent({
  props: {
    id: Number,
    updatedAt: String,
  },
  computed: {
    bgColor() {
      const colorOld = [150, 150, 150]
      const newColor = [208, 237, 23]
      const ageInMillisecond = Math.max(
        0,
        /* eslint-disable  @typescript-eslint/no-non-null-assertion */
        Date.now() - new Date(this.updatedAt!).getTime()
      )
      const max = 15 // equals to 225 hours
      const index = Math.min(max, Math.sqrt(ageInMillisecond / 1000 / 60 / 60))
      return `rgb(${colorOld
        .map((oc, i) =>
          Math.round((oc * index + newColor[i]! * (max - index)) / max)
        )
        .join(",")})`
    },
  },
})
</script>
