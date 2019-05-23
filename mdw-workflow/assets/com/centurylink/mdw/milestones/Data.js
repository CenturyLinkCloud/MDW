class Data {
    
  constructor(groups, milestones) {
    this.groups = groups;
    this.milestones = milestones;
    this.items = [];
    this.edges = [];
    this.idCtr = 0;
    this.depth = 0;
    this.maxDepth = 0;
    this.add(milestones);
  }
  
  add(milestone) {
    let item = milestone.milestone;
    item.id = this.idCtr;
    item.level = this.depth;
    if (item.group) {
      let group = item.group;
      delete item.group;
      let milestoneGroup = this.groups.find(g => g.name === group);
      if (milestoneGroup && milestoneGroup.props) {
        item.color = milestoneGroup.props.color;
      }
    }
    if (item.process) {
      item.title = item.process.name;
      if (item.activity) {
        item.title += ': ' + item.activity.id;
      }
    }
    this.depth++;
    this.items.push(item);
    if (milestone.children) {
      milestone.children.forEach(child => {
        let exist = this.items.find(it => {
          return it.process.id == child.milestone.process.id && it.activity.id == child.milestone.activity.id;
        });
        if (exist) {
          this.edges.push({
            from: item.id,
            to: exist.id
          });
        }
        else {
          this.edges.push({
            from: item.id,
            to: ++this.idCtr
          });
          this.add(child);
        }
      }, this);
    }
    if (this.depth > this.maxDepth) {
      this.maxDepth = this.depth;
    }
    this.depth--;
  }
}

export default Data; 